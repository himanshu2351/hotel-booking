package com.project.hotel_booking.service;

import com.project.hotel_booking.service.impl.PropertyDiscoveryService;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import com.project.hotel_booking.helper.search.PropertySearchFilter;
import com.project.hotel_booking.helper.search.RoomSearchFilter;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.utils.validation.PropertySearchValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyDiscoveryServiceTest {
    @Test
    void returnsOnlyPropertiesPassingPropertyAndRoomFilters() {
        HotelProperty.RoomType roomType = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 2, 10000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor Inn", "Mumbai",
                "Colaba", "Address", 4, Set.of("WIFI"), List.of(roomType));
        PropertyPortfolio portfolio = new PropertyPortfolio(UUID.randomUUID(), "Owner", "owner@example.com", List.of(property));
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        PropertySearchFilter propertyFilter = mock(PropertySearchFilter.class);
        RoomSearchFilter roomFilter = mock(RoomSearchFilter.class);
        when(portfolios.findAll()).thenReturn(List.of(portfolio));
        when(propertyFilter.matches(property, criteria())).thenReturn(true);
        when(roomFilter.matches(property, roomType, criteria())).thenReturn(true);
        PropertyDiscoveryService service = new PropertyDiscoveryService(
                portfolios, List.of(propertyFilter), List.of(roomFilter), new PropertySearchValidator());

        assertThat(service.search(criteria())).containsExactly(property);
    }

    @Test
    void excludesPropertyWhenRoomFilterRejectsItsRoomType() {
        HotelProperty.RoomType roomType = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 2, 10000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor Inn", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of(roomType));
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        RoomSearchFilter roomFilter = mock(RoomSearchFilter.class);
        when(portfolios.findAll()).thenReturn(List.of(new PropertyPortfolio(
                UUID.randomUUID(), "Owner", "owner@example.com", List.of(property))));
        when(roomFilter.matches(property, roomType, criteria())).thenReturn(false);
        PropertyDiscoveryService service = new PropertyDiscoveryService(
                portfolios, List.of(), List.of(roomFilter), new PropertySearchValidator());

        assertThat(service.search(criteria())).isEmpty();
    }

    private PropertySearchCriteria criteria() {
        return new PropertySearchCriteria(null, null, null, null, 1, null, null, null, Set.of());
    }
}
