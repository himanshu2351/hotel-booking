package com.project.hotel_booking.helper.pricing;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/** Default strategy uses the configured nightly rate without seasonal adjustments. */
@Component
@Primary
public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public long nightlyRateMinor(HotelProperty property, HotelProperty.RoomType roomType,
                                 LocalDate checkIn, LocalDate checkOut, int guests, int rooms) {
        return roomType.nightlyPriceMinor();
    }
}
