package com.project.hotel_booking.helper.cancellation;

import com.project.hotel_booking.helper.booking.Booking;

public record CancellationOutcome(Booking booking, long refundAmountMinor) {
}
