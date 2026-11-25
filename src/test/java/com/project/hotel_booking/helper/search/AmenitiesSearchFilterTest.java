package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AmenitiesSearchFilterTest {
    @Test
    void propertyMustContainEveryRequestedAmenity() {
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of("WIFI", "POOL"), List.of());
        AmenitiesSearchFilter filter = new AmenitiesSearchFilter();

        assertThat(filter.matches(property, criteria(Set.of("WIFI")))).isTrue();
        assertThat(filter.matches(property, criteria(Set.of("WIFI", "GYM")))).isFalse();
        assertThat(filter.matches(property, criteria(Set.of()))).isTrue();
    }

    private PropertySearchCriteria criteria(Set<String> amenities) {
        return new PropertySearchCriteria(null, null, null, null, 1, null, null, null, amenities);
    }
}
