package com.project.hotel_booking.helper.cancellation;

import com.project.hotel_booking.helper.booking.Booking;

public interface CancellationPolicy {
    long refundAmountMinor(Booking booking);
}
