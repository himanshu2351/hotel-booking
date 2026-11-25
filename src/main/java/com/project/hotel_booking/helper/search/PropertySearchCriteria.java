package com.project.hotel_booking.helper.search;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record PropertySearchCriteria(String city, String locality, LocalDate checkIn, LocalDate checkOut,
                                     int guests, Long minNightlyPriceMinor, Long maxNightlyPriceMinor,
                                     Integer minStarRating, Set<String> amenities) {
    public PropertySearchCriteria {
        amenities = amenities == null
                ? Set.of()
                : Collections.unmodifiableSet(new HashSet<>(amenities));
    }
}
