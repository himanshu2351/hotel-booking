package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.cancellation.CancellationOutcome;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.helper.payment.PaymentProcessor;
import com.project.hotel_booking.helper.payment.PaymentResult;
import com.project.hotel_booking.repo.InMemoryBookingRepository;
import com.project.hotel_booking.repo.InMemoryPaymentRecordRepository;
import com.project.hotel_booking.service.impl.CancellationService;
import com.project.hotel_booking.service.impl.PaymentService;
import com.project.hotel_booking.utils.IdempotencyLockManager;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.PaymentValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPaymentCancellationConcurrencyTest {
    @Test
    void serializesPaymentAndCancellationForTheSameInventory() throws Exception {
        Booking pendingBooking = pendingBooking();
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InMemoryPaymentRecordRepository payments = new InMemoryPaymentRecordRepository();
        bookings.save(pendingBooking);

        CountDownLatch gatewayChargeStarted = new CountDownLatch(1);
        CountDownLatch finishGatewayCharge = new CountDownLatch(1);
        CountDownLatch cancellationIsWaitingOnInventory = new CountDownLatch(1);
        AtomicInteger inventoryLockRequests = new AtomicInteger();
        InventoryLockManager lockManager = new InventoryLockManager() {
            @Override
            public <T> T withLock(UUID propertyId, UUID roomTypeId, Supplier<T> operation) {
                if (inventoryLockRequests.incrementAndGet() == 2) {
                    cancellationIsWaitingOnInventory.countDown();
                }
                return super.withLock(propertyId, roomTypeId, operation);
            }
        };
        PaymentProcessor slowProcessor = new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.CARD;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                gatewayChargeStarted.countDown();
                await(finishGatewayCharge);
                return new PaymentResult(true, "txn-race", "approved");
            }
        };
        PaymentService paymentService = new PaymentService(bookings, payments, List.of(slowProcessor), lockManager,
                new IdempotencyLockManager(), new PaymentValidator());
        CancellationService cancellationService = new CancellationService(bookings,
                booking -> booking.totalPriceMinor(), lockManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<PaymentOutcome> payment = executor.submit(() -> paymentService.pay(
                    pendingBooking.id(), new com.project.hotel_booking.helper.payment.PaymentRequest(PaymentMethod.CARD),
                    "race-key"));
            assertThat(gatewayChargeStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<CancellationOutcome> cancellation = executor.submit(
                    () -> cancellationService.cancel(pendingBooking.id()));
            assertThat(cancellationIsWaitingOnInventory.await(5, TimeUnit.SECONDS)).isTrue();

            finishGatewayCharge.countDown();
            assertThat(payment.get(5, TimeUnit.SECONDS).booking().status()).isEqualTo(BookingStatus.CONFIRMED);
            CancellationOutcome outcome = cancellation.get(5, TimeUnit.SECONDS);

            assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(outcome.refundAmountMinor()).isEqualTo(pendingBooking.totalPriceMinor());
            assertThat(bookings.findById(pendingBooking.id()).orElseThrow().status())
                    .isEqualTo(BookingStatus.CANCELLED);
        } finally {
            finishGatewayCharge.countDown();
            executor.shutdownNow();
        }
    }

    private Booking pendingBooking() {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Guest", "guest@example.com",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2, 1, 12000L,
                BookingStatus.PENDING_PAYMENT, Instant.now());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for gateway test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting in test", exception);
        }
    }
}
