package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocationSearchFilterTest {
    @Test
    void matchesCityAndLocalityCaseInsensitivelyAndAllowsOmittedLocationCriteria() {
        HotelProperty property = property();
        LocationSearchFilter filter = new LocationSearchFilter();
        assertThat(filter.matches(property, criteria("MUM", "cola"))).isTrue();
        assertThat(filter.matches(property, criteria("Delhi", null))).isFalse();
        assertThat(filter.matches(property, criteria(null, "Bandra"))).isFalse();
        assertThat(filter.matches(property, criteria(null, null))).isTrue();
    }

    private HotelProperty property() {
        return new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai", "Colaba", "Address",
                4, Set.of(), List.of());
    }

    private PropertySearchCriteria criteria(String city, String locality) {
        return new PropertySearchCriteria(city, locality, null, null, 1, null, null, null, Set.of());
    }
}
