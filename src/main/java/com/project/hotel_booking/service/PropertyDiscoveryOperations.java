package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;

import java.util.List;

/** Application boundary for searching available properties. */
public interface PropertyDiscoveryOperations {
    List<HotelProperty> search(PropertySearchCriteria criteria);
}
