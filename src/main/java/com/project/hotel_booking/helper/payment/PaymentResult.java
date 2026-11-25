package com.project.hotel_booking.helper.payment;

public record PaymentResult(boolean successful, String transactionId, String message) {
}
