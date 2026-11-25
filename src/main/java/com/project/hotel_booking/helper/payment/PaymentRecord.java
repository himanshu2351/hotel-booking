package com.project.hotel_booking.helper.payment;

import com.project.hotel_booking.helper.booking.Booking;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(
        String idempotencyKey,
        UUID bookingId,
        PaymentMethod method,
        Booking booking,
        PaymentResult payment,
        Instant createdAt
) {
}
