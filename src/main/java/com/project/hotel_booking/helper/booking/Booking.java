package com.project.hotel_booking.helper.booking;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record Booking(UUID id, UUID propertyId, UUID roomTypeId, String guestName, String guestEmail,
                      LocalDate checkIn, LocalDate checkOut, int guests, int rooms,
                      long totalPriceMinor, BookingStatus status, Instant createdAt) {
    public Booking withStatus(BookingStatus nextStatus) {
        return new Booking(id, propertyId, roomTypeId, guestName, guestEmail, checkIn, checkOut,
                guests, rooms, totalPriceMinor, nextStatus, createdAt);
    }
}
