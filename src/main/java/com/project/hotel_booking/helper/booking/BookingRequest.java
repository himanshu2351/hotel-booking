package com.project.hotel_booking.helper.booking;

import java.time.LocalDate;
import java.util.UUID;

public record BookingRequest(
        UUID propertyId,
        UUID roomTypeId,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        int rooms
) {
}
