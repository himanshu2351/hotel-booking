package com.project.hotel_booking.helper.property;

import java.util.List;
import java.util.UUID;

/** An owner account; a standalone owner is represented by a portfolio with one property. */
public record PropertyPortfolio(UUID id, String ownerName, String ownerEmail, List<HotelProperty> properties) {
    public PropertyPortfolio {
        properties = List.copyOf(properties);
    }
}
