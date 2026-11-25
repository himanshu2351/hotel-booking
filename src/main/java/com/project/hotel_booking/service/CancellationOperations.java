package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.cancellation.CancellationOutcome;

import java.util.UUID;

/** Application boundary for cancelling guest bookings. */
public interface CancellationOperations {
    CancellationOutcome cancel(UUID bookingId);
}
