package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import com.project.hotel_booking.service.PropertyDiscoveryOperations;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyDiscoveryControllerTest {
    private final PropertyDiscoveryOperations discoveryService = mock(PropertyDiscoveryOperations.class);
    private final PropertyDiscoveryController controller = new PropertyDiscoveryController(discoveryService);

    @Test
    void createsCriteriaAndUsesEmptyAmenitiesWhenNoneWereProvided() {
        List<HotelProperty> properties = List.of(mock(HotelProperty.class));
        when(discoveryService.search(any(PropertySearchCriteria.class))).thenReturn(properties);

        List<HotelProperty> result = controller.search(
                "Mumbai", "Colaba", LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3),
                2, 100L, 500L, 4, null
        );

        assertThat(result).isSameAs(properties);
        verify(discoveryService).search(new PropertySearchCriteria(
                "Mumbai", "Colaba", LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3),
                2, 100L, 500L, 4, Set.of()
        ));
    }

    @Test
    void preservesSuppliedAmenities() {
        when(discoveryService.search(any(PropertySearchCriteria.class))).thenReturn(List.of());
        Set<String> amenities = Set.of("WIFI");

        controller.search(null, null, null, null, 1, null, null, null, amenities);

        verify(discoveryService).search(new PropertySearchCriteria(null, null, null, null,
                1, null, null, null, amenities));
    }
}
