package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PropertySearchValidator {
    public void validate(PropertySearchCriteria criteria) {
        if (criteria == null) {
            throw new BadRequestException("Search criteria are required");
        }

        validateDates(criteria.checkIn(), criteria.checkOut());

        if (criteria.guests() < 1) {
            throw new BadRequestException("Guests must be at least one");
        }

        if (criteria.minStarRating() != null
                && (criteria.minStarRating() < 1 || criteria.minStarRating() > 5)) {
            throw new BadRequestException("Minimum star rating must be between 1 and 5");
        }

        if (criteria.amenities().stream().anyMatch(amenity -> amenity == null || amenity.isBlank())) {
            throw new BadRequestException("Amenity filters cannot be blank");
        }

        if ((criteria.minNightlyPriceMinor() != null && criteria.minNightlyPriceMinor() < 0)
                || (criteria.maxNightlyPriceMinor() != null && criteria.maxNightlyPriceMinor() < 0)) {
            throw new BadRequestException("Price filters cannot be negative");
        }

        if (criteria.minNightlyPriceMinor() != null && criteria.maxNightlyPriceMinor() != null
                && criteria.minNightlyPriceMinor() > criteria.maxNightlyPriceMinor()) {
            throw new BadRequestException("Minimum price cannot exceed maximum price");
        }
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new BadRequestException("Check-in and check-out must be provided together");
        }

        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out must be after check-in");
        }

        if (checkIn != null && checkIn.isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in cannot be in the past");
        }
    }
}
