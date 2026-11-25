package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.springframework.stereotype.Component;

@Component
public class AmenitiesSearchFilter implements PropertySearchFilter {
    public boolean matches(HotelProperty property, PropertySearchCriteria criteria) {
        return property.amenities().containsAll(criteria.amenities());
    }
}
