package com.project.hotel_booking.service;

import com.project.hotel_booking.exception.PaymentProcessingException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.helper.payment.PaymentProcessor;
import com.project.hotel_booking.helper.payment.PaymentRecord;
import com.project.hotel_booking.helper.payment.PaymentRequest;
import com.project.hotel_booking.helper.payment.PaymentResult;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.repo.InMemoryBookingRepository;
import com.project.hotel_booking.repo.InMemoryPaymentRecordRepository;
import com.project.hotel_booking.repo.PaymentRecordRepository;
import com.project.hotel_booking.utils.IdempotencyLockManager;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.PaymentValidator;
import com.project.hotel_booking.service.impl.PaymentService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    @Test
    void chargesPendingBookingAndConfirmsIt() {
        UUID bookingId = UUID.randomUUID();
        Booking pendingBooking = booking(bookingId, BookingStatus.PENDING_PAYMENT);
        BookingRepository bookings = mock(BookingRepository.class);
        PaymentRecordRepository paymentRecords = mock(PaymentRecordRepository.class);
        PaymentProcessor processor = mock(PaymentProcessor.class);
        when(processor.method()).thenReturn(PaymentMethod.CARD);
        when(processor.charge(bookingId, 25000L, "key-1"))
                .thenReturn(new PaymentResult(true, "txn-1", "approved"));
        when(bookings.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRecords.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRecords.save(any(PaymentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentService service = service(bookings, paymentRecords, List.of(processor));

        PaymentOutcome outcome = service.pay(bookingId, new PaymentRequest(PaymentMethod.CARD), "key-1");

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(outcome.payment().transactionId()).isEqualTo("txn-1");
        verify(paymentRecords).save(any(PaymentRecord.class));
    }

    @Test
    void allowsRetryWithSameKeyWhenProcessorThrowsBeforeReturningAnOutcome() {
        Booking pendingBooking = booking(UUID.randomUUID(), BookingStatus.PENDING_PAYMENT);
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InMemoryPaymentRecordRepository paymentRecords = new InMemoryPaymentRecordRepository();
        bookings.save(pendingBooking);
        AtomicInteger attempts = new AtomicInteger();
        PaymentProcessor flakyProcessor = new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.CARD;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("Gateway timeout");
                }
                return new PaymentResult(true, "txn-retry", "approved");
            }
        };
        PaymentService service = service(bookings, paymentRecords, List.of(flakyProcessor));

        assertThatThrownBy(() -> service.pay(pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "retry-key"))
                .isInstanceOf(PaymentProcessingException.class);
        assertThat(bookings.findById(pendingBooking.id()).orElseThrow().status())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(paymentRecords.findByIdempotencyKey("retry-key")).isEmpty();

        PaymentOutcome retry = service.pay(
                pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "retry-key");

        assertThat(retry.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void simultaneousRequestsWithTheSameIdempotencyKeyChargeOnlyOnce() throws Exception {
        Booking pendingBooking = booking(UUID.randomUUID(), BookingStatus.PENDING_PAYMENT);
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InMemoryPaymentRecordRepository paymentRecords = new InMemoryPaymentRecordRepository();
        bookings.save(pendingBooking);
        AtomicInteger chargeCount = new AtomicInteger();
        CountDownLatch chargeStarted = new CountDownLatch(1);
        CountDownLatch allowChargeToFinish = new CountDownLatch(1);
        PaymentProcessor slowProcessor = new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.CARD;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                chargeCount.incrementAndGet();
                chargeStarted.countDown();
                await(allowChargeToFinish);
                return new PaymentResult(true, "txn-once", "approved");
            }
        };
        PaymentService service = service(bookings, paymentRecords, List.of(slowProcessor));
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<PaymentOutcome> first = executor.submit(() -> service.pay(
                    pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), " shared-key "));
            assertThat(chargeStarted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<PaymentOutcome> second = executor.submit(() -> service.pay(
                    pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "shared-key"));
            allowChargeToFinish.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(second.get(5, TimeUnit.SECONDS));
            assertThat(chargeCount).hasValue(1);
            assertThat(paymentRecords.findByIdempotencyKey("shared-key")).isPresent();
        } finally {
            allowChargeToFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void refusesMoreThanOneProcessorForTheSamePaymentMethod() {
        PaymentProcessor firstProcessor = processor(PaymentMethod.CARD);
        PaymentProcessor duplicateProcessor = processor(PaymentMethod.CARD);

        assertThatThrownBy(() -> service(mock(BookingRepository.class), mock(PaymentRecordRepository.class),
                List.of(firstProcessor, duplicateProcessor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one payment processor");
    }

    @Test
    void treatsMissingProcessorResultAsAnUnknownPaymentOutcome() {
        Booking pendingBooking = booking(UUID.randomUUID(), BookingStatus.PENDING_PAYMENT);
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        bookings.save(pendingBooking);
        PaymentProcessor processorWithoutResult = new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.CARD;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                return null;
            }
        };
        PaymentService service = service(bookings, new InMemoryPaymentRecordRepository(),
                List.of(processorWithoutResult));

        assertThatThrownBy(() -> service.pay(pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "null-result"))
                .isInstanceOf(PaymentProcessingException.class);
        assertThat(bookings.findById(pendingBooking.id()).orElseThrow().status())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    private PaymentService service(BookingRepository bookings, PaymentRecordRepository paymentRecords,
                                   List<PaymentProcessor> processors) {
        return new PaymentService(bookings, paymentRecords, processors, new InventoryLockManager(),
                new IdempotencyLockManager(), new PaymentValidator());
    }

    private PaymentProcessor processor(PaymentMethod method) {
        return new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return method;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                return new PaymentResult(true, "txn", "approved");
            }
        };
    }

    private Booking booking(UUID id, BookingStatus status) {
        return new Booking(id, UUID.randomUUID(), UUID.randomUUID(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 1, 25000L, status, Instant.now());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting in test", exception);
        }
    }
}
