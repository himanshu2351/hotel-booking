package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.utils.InventoryAvailability;
import org.springframework.stereotype.Component;

@Component
public class RoomAvailabilitySearchFilter implements RoomSearchFilter {
    private final BookingRepository bookings;
    private final InventoryAvailability inventoryAvailability;

    public RoomAvailabilitySearchFilter(BookingRepository bookings, InventoryAvailability inventoryAvailability) {
        this.bookings = bookings;
        this.inventoryAvailability = inventoryAvailability;
    }

    @Override
    public boolean matches(HotelProperty property, HotelProperty.RoomType roomType, PropertySearchCriteria criteria) {
        int roomsNeeded = (int) (((long) criteria.guests() + roomType.maxGuests() - 1) / roomType.maxGuests());
        long roomsAvailable = criteria.checkIn() == null
                ? roomType.inventoryCount()
                : inventoryAvailability.availableRooms(
                        property.id(),
                        roomType,
                        criteria.checkIn(),
                        criteria.checkOut(),
                        bookings.findAll()
                );
        return roomsAvailable >= roomsNeeded;
    }
}
