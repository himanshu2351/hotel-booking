package com.project.hotel_booking.utils;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.property.HotelProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Shared inventory calculation for search and booking operations. */
@Component
public class InventoryAvailability {
    public long availableRooms(UUID propertyId, HotelProperty.RoomType roomType,
                               LocalDate checkIn, LocalDate checkOut, List<Booking> bookings) {
        List<Booking> overlappingBookings = bookings.stream()
                .filter(booking -> booking.propertyId().equals(propertyId))
                .filter(booking -> booking.roomTypeId().equals(roomType.id()))
                .filter(booking -> booking.status() == BookingStatus.PENDING_PAYMENT
                        || booking.status() == BookingStatus.CONFIRMED)
                .filter(booking -> booking.checkIn().isBefore(checkOut) && checkIn.isBefore(booking.checkOut()))
                .toList();
        long peakBookedRooms = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            LocalDate currentNight = night;
            long bookedThatNight = overlappingBookings.stream()
                    .filter(booking -> !currentNight.isBefore(booking.checkIn())
                            && currentNight.isBefore(booking.checkOut()))
                    .mapToLong(Booking::rooms).sum();
            peakBookedRooms = Math.max(peakBookedRooms, bookedThatNight);
        }
        return roomType.inventoryCount() - peakBookedRooms;
    }
}
