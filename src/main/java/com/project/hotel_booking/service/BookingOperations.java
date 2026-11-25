package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;

import java.util.UUID;

/** Application boundary for creating and reading guest bookings. */
public interface BookingOperations {
    Booking createBooking(BookingRequest request);

    Booking booking(UUID id);
}
