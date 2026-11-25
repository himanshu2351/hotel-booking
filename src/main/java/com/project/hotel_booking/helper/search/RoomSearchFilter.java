package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;

/** A filter evaluated against one room type, keeping related room criteria together. */
public interface RoomSearchFilter {
    boolean matches(HotelProperty property, HotelProperty.RoomType roomType, PropertySearchCriteria criteria);
}
