package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.utils.InventoryAvailability;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomAvailabilitySearchFilterTest {
    @Test
    void checksInventoryCountForBrowseAndAvailableRoomsForDatedSearch() {
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 3, 10000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of(room));
        BookingRepository bookings = mock(BookingRepository.class);
        InventoryAvailability availability = mock(InventoryAvailability.class);
        when(bookings.findAll()).thenReturn(List.of());
        LocalDate checkIn = LocalDate.of(2031, 5, 1);
        LocalDate checkOut = LocalDate.of(2031, 5, 2);
        when(availability.availableRooms(property.id(), room, checkIn, checkOut, List.of())).thenReturn(3L);
        RoomAvailabilitySearchFilter filter = new RoomAvailabilitySearchFilter(bookings, availability);

        assertThat(filter.matches(property, room, criteria(5, null, null))).isTrue();
        assertThat(filter.matches(property, room, criteria(7, null, null))).isFalse();
        assertThat(filter.matches(property, room, criteria(5, checkIn, checkOut))).isTrue();
        when(availability.availableRooms(property.id(), room, checkIn, checkOut, List.of())).thenReturn(2L);
        assertThat(filter.matches(property, room, criteria(5, checkIn, checkOut))).isFalse();
    }

    private PropertySearchCriteria criteria(int guests, LocalDate checkIn, LocalDate checkOut) {
        return new PropertySearchCriteria(null, null, checkIn, checkOut, guests, null, null, null, Set.of());
    }
}
