package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.helper.payment.PaymentRequest;

import java.util.UUID;

/** Application boundary for taking payment against a booking. */
public interface PaymentOperations {
    PaymentOutcome pay(UUID bookingId, PaymentRequest request, String idempotencyKey);
}
