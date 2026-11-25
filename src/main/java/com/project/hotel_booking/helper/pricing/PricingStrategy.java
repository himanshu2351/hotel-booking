package com.project.hotel_booking.helper.pricing;

import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.helper.property.HotelProperty;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public interface PricingStrategy {
    /**
     * Returns the effective nightly rate for this room and stay. Dates may be null during browse-only searches.
     */
    long nightlyRateMinor(HotelProperty property, HotelProperty.RoomType roomType,
                          LocalDate checkIn, LocalDate checkOut, int guests, int rooms);

    default long totalPriceMinor(HotelProperty property, HotelProperty.RoomType roomType, BookingRequest request) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        long nightlyRate = nightlyRateMinor(
                property,
                roomType,
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.rooms()
        );
        return Math.multiplyExact(Math.multiplyExact(nightlyRate, nights), request.rooms());
    }
}
