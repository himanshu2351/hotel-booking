package com.project.hotel_booking.service.impl;

import com.project.hotel_booking.service.PaymentOperations;
import com.project.hotel_booking.exception.PaymentProcessingException;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.helper.payment.PaymentProcessor;
import com.project.hotel_booking.helper.payment.PaymentRecord;
import com.project.hotel_booking.helper.payment.PaymentRequest;
import com.project.hotel_booking.helper.payment.PaymentResult;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.repo.PaymentRecordRepository;
import com.project.hotel_booking.utils.IdempotencyLockManager;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.PaymentValidator;
import org.springframework.stereotype.Service;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService implements PaymentOperations {
    private final BookingRepository bookings;
    private final PaymentRecordRepository paymentRecords;
    private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);
    private final InventoryLockManager inventoryLockManager;
    private final IdempotencyLockManager idempotencyLockManager;
    private final PaymentValidator validator;

    public PaymentService(BookingRepository bookings, PaymentRecordRepository paymentRecords,
                          List<PaymentProcessor> processors, InventoryLockManager inventoryLockManager,
                          IdempotencyLockManager idempotencyLockManager, PaymentValidator validator) {
        this.bookings = bookings;
        this.paymentRecords = paymentRecords;
        this.inventoryLockManager = inventoryLockManager;
        this.idempotencyLockManager = idempotencyLockManager;
        this.validator = validator;
        processors.forEach(processor -> {
            PaymentProcessor existingProcessor = this.processors.putIfAbsent(processor.method(), processor);
            if (existingProcessor != null) {
                throw new IllegalArgumentException("Only one payment processor can be registered per method");
            }
        });
    }

    @Override
    public PaymentOutcome pay(UUID bookingId, PaymentRequest request, String idempotencyKey) {
        PaymentMethod method = validator.validate(request, processors.keySet());
        String normalizedKey = validator.validateIdempotencyKey(idempotencyKey);
        return idempotencyLockManager.withLock(
                normalizedKey,
                () -> processPayment(bookingId, method, normalizedKey)
        );
    }

    private PaymentOutcome processPayment(UUID bookingId, PaymentMethod method, String idempotencyKey) {
        PaymentRecord previousPayment = paymentRecords.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (previousPayment != null) {
            if (!previousPayment.bookingId().equals(bookingId) || previousPayment.method() != method) {
                throw new ConflictException("Idempotency-Key was already used for a different payment request");
            }
            return new PaymentOutcome(previousPayment.booking(), previousPayment.payment());
        }

        Booking initialBooking = findBooking(bookingId);
        return inventoryLockManager.withLock(initialBooking.propertyId(), initialBooking.roomTypeId(), () -> {
            Booking booking = findBooking(bookingId);
            if (booking.status() != BookingStatus.PENDING_PAYMENT) {
                throw new ConflictException("Booking is not awaiting payment");
            }

            PaymentProcessor processor = processors.get(method);
            PaymentResult result;
            try {
                result = processor.charge(bookingId, booking.totalPriceMinor(), idempotencyKey);
                if (result == null) {
                    throw new IllegalStateException("Payment processor returned no result");
                }
            } catch (RuntimeException exception) {
                throw new PaymentProcessingException(
                        "Payment outcome could not be confirmed; retry with the same idempotency key",
                        exception
                );
            }
            BookingStatus nextStatus = result.successful()
                    ? BookingStatus.CONFIRMED
                    : BookingStatus.PAYMENT_FAILED;
            Booking updatedBooking = booking.withStatus(nextStatus);
            bookings.save(updatedBooking);

            PaymentRecord paymentRecord = new PaymentRecord(
                    idempotencyKey,
                    bookingId,
                    method,
                    updatedBooking,
                    result,
                    java.time.Instant.now()
            );
            paymentRecords.save(paymentRecord);
            return new PaymentOutcome(updatedBooking, result);
        });
    }

    private Booking findBooking(UUID bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
