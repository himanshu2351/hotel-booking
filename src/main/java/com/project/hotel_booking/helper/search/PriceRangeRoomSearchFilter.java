package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.pricing.PricingStrategy;
import org.springframework.stereotype.Component;

@Component
public class PriceRangeRoomSearchFilter implements RoomSearchFilter {
    private final PricingStrategy pricingStrategy;

    public PriceRangeRoomSearchFilter(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    @Override
    public boolean matches(HotelProperty property, HotelProperty.RoomType roomType, PropertySearchCriteria criteria) {
        int roomsNeeded = (int) (((long) criteria.guests() + roomType.maxGuests() - 1) / roomType.maxGuests());
        long nightlyRate = pricingStrategy.nightlyRateMinor(
                property,
                roomType,
                criteria.checkIn(),
                criteria.checkOut(),
                criteria.guests(),
                roomsNeeded
        );
        boolean meetsMinimum = criteria.minNightlyPriceMinor() == null
                || nightlyRate >= criteria.minNightlyPriceMinor();
        boolean meetsMaximum = criteria.maxNightlyPriceMinor() == null
                || nightlyRate <= criteria.maxNightlyPriceMinor();
        return meetsMinimum && meetsMaximum;
    }
}
