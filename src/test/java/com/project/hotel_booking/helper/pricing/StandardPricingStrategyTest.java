package com.project.hotel_booking.helper.pricing;

import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.helper.property.HotelProperty;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardPricingStrategyTest {
    @Test
    void usesConfiguredNightlyRateAndCalculatesStayTotal() {
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 3, 12000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of(room));
        StandardPricingStrategy strategy = new StandardPricingStrategy();
        BookingRequest request = new BookingRequest(property.id(), room.id(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 4), 2, 2);

        assertThat(strategy.nightlyRateMinor(property, room, request.checkIn(), request.checkOut(), 2, 2))
                .isEqualTo(12000L);
        assertThat(strategy.totalPriceMinor(property, room, request)).isEqualTo(72000L);
    }

    @Test
    void detectsPriceOverflow() {
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Suite", 2, 3, Long.MAX_VALUE);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor", "Mumbai",
                "Colaba", "Address", 4, Set.of(), List.of(room));
        BookingRequest request = new BookingRequest(property.id(), room.id(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 2);

        assertThatThrownBy(() -> new StandardPricingStrategy().totalPriceMinor(property, room, request))
                .isInstanceOf(ArithmeticException.class);
    }
}
