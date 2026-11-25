package com.project.hotel_booking.helper.property;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record HotelProperty(UUID id, UUID portfolioId, String name, String city, String locality,
                            String address, int starRating, Set<String> amenities, List<RoomType> roomTypes) {
    public HotelProperty {
        amenities = Set.copyOf(amenities);
        roomTypes = List.copyOf(roomTypes);
    }

    public record RoomType(UUID id, String name, int maxGuests, int inventoryCount, long nightlyPriceMinor) {
    }
}
