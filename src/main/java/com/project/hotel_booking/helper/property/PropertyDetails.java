package com.project.hotel_booking.helper.property;

import java.util.List;
import java.util.Set;

public record PropertyDetails(
        String name,
        String city,
        String locality,
        String address,
        int starRating,
        Set<String> amenities,
        List<RoomTypeDetails> roomTypes
) {
    public record RoomTypeDetails(
            String name,
            int maxGuests,
            int inventoryCount,
            long nightlyPriceMinor
    ) {
    }
}
