package com.project.hotel_booking.utils;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAvailabilityTest {
    @Test
    void calculatesPeakOccupancyAcrossOverlappingBookingsAndIgnoresOtherInventory() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        HotelProperty.RoomType room = new HotelProperty.RoomType(roomTypeId, "Queen", 2, 5, 10000L);
        LocalDate firstNight = LocalDate.of(2031, 5, 1);
        LocalDate thirdNight = firstNight.plusDays(2);
        List<Booking> bookings = List.of(
                booking(propertyId, roomTypeId, firstNight, firstNight.plusDays(2), 2, BookingStatus.CONFIRMED),
                booking(propertyId, roomTypeId, firstNight.plusDays(1), thirdNight, 1, BookingStatus.PENDING_PAYMENT),
                booking(propertyId, roomTypeId, firstNight, thirdNight, 3, BookingStatus.CANCELLED),
                booking(UUID.randomUUID(), roomTypeId, firstNight, thirdNight, 3, BookingStatus.CONFIRMED)
        );

        assertThat(new InventoryAvailability().availableRooms(propertyId, room, firstNight, thirdNight, bookings))
                .isEqualTo(2);
    }

    @Test
    void returnsFullInventoryWhenNoBookingOverlapsRequestedDates() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        HotelProperty.RoomType room = new HotelProperty.RoomType(roomTypeId, "Queen", 2, 5, 10000L);
        LocalDate start = LocalDate.of(2031, 5, 10);
        Booking earlierBooking = booking(propertyId, roomTypeId, start.minusDays(2), start, 5, BookingStatus.CONFIRMED);

        assertThat(new InventoryAvailability().availableRooms(propertyId, room, start, start.plusDays(1),
                List.of(earlierBooking))).isEqualTo(5);
    }

    @Test
    void doesNotOverflowWhenExistingBookingsExceedIntegerInventory() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        HotelProperty.RoomType room = new HotelProperty.RoomType(roomTypeId, "Queen", 2, 2_000_000_000,
                10000L);
        LocalDate start = LocalDate.of(2031, 5, 10);
        List<Booking> bookings = List.of(
                booking(propertyId, roomTypeId, start, start.plusDays(1), 1_500_000_000,
                        BookingStatus.CONFIRMED),
                booking(propertyId, roomTypeId, start, start.plusDays(1), 1_500_000_000,
                        BookingStatus.CONFIRMED),
                booking(propertyId, roomTypeId, start, start.plusDays(1), 1_500_000_000,
                        BookingStatus.CONFIRMED)
        );

        assertThat(new InventoryAvailability().availableRooms(propertyId, room, start, start.plusDays(1), bookings))
                .isEqualTo(-2_500_000_000L);
    }

    private Booking booking(UUID propertyId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
                            int rooms, BookingStatus status) {
        return new Booking(UUID.randomUUID(), propertyId, roomTypeId, "Guest", "guest@example.com",
                checkIn, checkOut, 1, rooms, 10000L, status, Instant.now());
    }
}
