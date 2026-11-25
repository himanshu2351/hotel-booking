package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.springframework.stereotype.Component;

@Component
public class StarRatingSearchFilter implements PropertySearchFilter {
    public boolean matches(HotelProperty property, PropertySearchCriteria criteria) {
        return criteria.minStarRating() == null || property.starRating() >= criteria.minStarRating();
    }
}
