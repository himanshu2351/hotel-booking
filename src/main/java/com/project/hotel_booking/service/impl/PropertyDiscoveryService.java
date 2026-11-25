package com.project.hotel_booking.service.impl;

import com.project.hotel_booking.service.PropertyDiscoveryOperations;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import com.project.hotel_booking.helper.search.PropertySearchFilter;
import com.project.hotel_booking.helper.search.RoomSearchFilter;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.utils.validation.PropertySearchValidator;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PropertyDiscoveryService implements PropertyDiscoveryOperations {
    private final PortfolioRepository portfolios;
    private final List<PropertySearchFilter> filters;
    private final List<RoomSearchFilter> roomFilters;
    private final PropertySearchValidator validator;

    public PropertyDiscoveryService(PortfolioRepository portfolios,
                                   List<PropertySearchFilter> filters,
                                   List<RoomSearchFilter> roomFilters,
                                   PropertySearchValidator validator) {
        this.portfolios = portfolios;
        this.filters = filters;
        this.roomFilters = roomFilters;
        this.validator = validator;
    }

    @Override
    public List<HotelProperty> search(PropertySearchCriteria criteria) {
        validator.validate(criteria);
        return portfolios.findAll().stream().flatMap(portfolio -> portfolio.properties().stream())
                .filter(property -> filters.stream().allMatch(filter -> filter.matches(property, criteria)))
                .filter(property -> hasMatchingRoomType(property, criteria))
                .toList();
    }

    private boolean hasMatchingRoomType(HotelProperty property, PropertySearchCriteria criteria) {
        return property.roomTypes().stream().anyMatch(roomType ->
                roomFilters.stream().allMatch(filter -> filter.matches(property, roomType, criteria))
        );
    }
}
