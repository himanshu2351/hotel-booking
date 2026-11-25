package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StarRatingSearchFilterTest {
    @Test
    void comparesRatingToOptionalMinimum() {
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of());
        StarRatingSearchFilter filter = new StarRatingSearchFilter();

        assertThat(filter.matches(property, criteria(4))).isTrue();
        assertThat(filter.matches(property, criteria(5))).isFalse();
        assertThat(filter.matches(property, criteria(null))).isTrue();
    }

    private PropertySearchCriteria criteria(Integer minRating) {
        return new PropertySearchCriteria(null, null, null, null, 1, null, null, minRating, Set.of());
    }
}
