package com.project.hotel_booking.helper.search;

import com.project.hotel_booking.helper.pricing.PricingStrategy;
import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceRangeRoomSearchFilterTest {
    @Test
    void appliesPriceBoundsToStrategyRateAndCalculatesRoomsNeeded() {
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 4, 10000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of(room));
        PricingStrategy pricing = mock(PricingStrategy.class);
        when(pricing.nightlyRateMinor(property, room, null, null, 3, 2)).thenReturn(12000L);
        PriceRangeRoomSearchFilter filter = new PriceRangeRoomSearchFilter(pricing);

        assertThat(filter.matches(property, room, criteria(12000L, 12000L, 3))).isTrue();
        assertThat(filter.matches(property, room, criteria(12001L, null, 3))).isFalse();
        assertThat(filter.matches(property, room, criteria(null, 11999L, 3))).isFalse();
    }

    private PropertySearchCriteria criteria(Long minimum, Long maximum, int guests) {
        return new PropertySearchCriteria(null, null, null, null, guests, minimum, maximum, null, Set.of());
    }
}
