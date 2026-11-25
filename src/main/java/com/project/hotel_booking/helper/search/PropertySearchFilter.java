package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;

/** Search criteria are decomposed into filters so new filter types can be added independently. */
public interface PropertySearchFilter {
    boolean matches(HotelProperty property, PropertySearchCriteria criteria);
}
