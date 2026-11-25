package com.project.hotel_booking.helper.payment;

import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentResult;
import java.util.UUID;

public interface PaymentProcessor {
    PaymentMethod method();
    PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey);
}
