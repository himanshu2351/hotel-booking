package com.project.hotel_booking.helper.payment;

import com.project.hotel_booking.helper.booking.Booking;

public record PaymentOutcome(Booking booking, PaymentResult payment) {
}
