package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocationSearchFilter implements PropertySearchFilter {
    public boolean matches(HotelProperty property, PropertySearchCriteria criteria) {
        return contains(property.city(), criteria.city()) && contains(property.locality(), criteria.locality());
    }
    private boolean contains(String value, String query) {
        return query == null || value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }
}
