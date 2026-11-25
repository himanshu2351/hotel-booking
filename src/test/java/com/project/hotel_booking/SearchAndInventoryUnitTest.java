package com.project.hotel_booking;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.search.AmenitiesSearchFilter;
import com.project.hotel_booking.helper.search.LocationSearchFilter;
import com.project.hotel_booking.helper.search.PriceRangeRoomSearchFilter;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import com.project.hotel_booking.helper.search.RoomAvailabilitySearchFilter;
import com.project.hotel_booking.helper.search.StarRatingSearchFilter;
import com.project.hotel_booking.helper.pricing.StandardPricingStrategy;
import com.project.hotel_booking.repo.InMemoryBookingRepository;
import com.project.hotel_booking.utils.InventoryAvailability;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class SearchAndInventoryUnitTest {
    private static final LocalDate FIRST_NIGHT = LocalDate.of(2030, 4, 1);
    private static final LocalDate THIRD_NIGHT = LocalDate.of(2030, 4, 3);
    private static final LocalDate FIFTH_NIGHT = LocalDate.of(2030, 4, 5);

    private final UUID propertyId = UUID.randomUUID();
    private final UUID roomTypeId = UUID.randomUUID();
    private final HotelProperty property = new HotelProperty(
            propertyId, UUID.randomUUID(), "Bay Hotel", "Mumbai", "Colaba", "",
            4, Set.of("WIFI", "POOL"), List.of()
    );
    private final HotelProperty.RoomType roomType = new HotelProperty.RoomType(
            roomTypeId, "Family", 2, 3, 12000L
    );

    @Test
    void matchesLocationAmenitiesStarsAndPriceWithOptionalCriteria() {
        LocationSearchFilter locationFilter = new LocationSearchFilter();
        assertThat(locationFilter.matches(property, locationCriteria(null, null))).isTrue();
        assertThat(locationFilter.matches(property, locationCriteria("mumb", "COL"))).isTrue();
        assertThat(locationFilter.matches(property, locationCriteria("Delhi", "Colaba"))).isFalse();
        assertThat(locationFilter.matches(property, locationCriteria("Mumbai", "Band"))).isFalse();

        AmenitiesSearchFilter amenitiesFilter = new AmenitiesSearchFilter();
        assertThat(amenitiesFilter.matches(property, criteria(null, null, 1, null, null, null, Set.of()))).isTrue();
        assertThat(amenitiesFilter.matches(property, criteria(null, null, 1, null, null, null, Set.of("WIFI")))).isTrue();
        assertThat(amenitiesFilter.matches(property, criteria(null, null, 1, null, null, null, Set.of("SPA")))).isFalse();

        StarRatingSearchFilter starFilter = new StarRatingSearchFilter();
        assertThat(starFilter.matches(property, criteria(null, null, 1, null, null, null, Set.of()))).isTrue();
        assertThat(starFilter.matches(property, criteria(null, null, 1, null, null, 5, Set.of()))).isFalse();
        assertThat(starFilter.matches(property, criteria(null, null, 1, null, null, 4, Set.of()))).isTrue();

        PriceRangeRoomSearchFilter priceFilter = new PriceRangeRoomSearchFilter(new StandardPricingStrategy());
        assertThat(priceFilter.matches(property, roomType,
                criteria(null, null, 2, null, null, null, Set.of()))).isTrue();
        assertThat(priceFilter.matches(property, roomType,
                criteria(null, null, 2, 12000L, null, null, Set.of()))).isTrue();
        assertThat(priceFilter.matches(property, roomType,
                criteria(null, null, 2, null, 12000L, null, Set.of()))).isTrue();
        assertThat(priceFilter.matches(property, roomType,
                criteria(null, null, 2, 12001L, null, null, Set.of()))).isFalse();
        assertThat(priceFilter.matches(property, roomType,
                criteria(null, null, 2, null, 11999L, null, Set.of()))).isFalse();
    }

    @Test
    void calculatesPeakRoomAvailabilityAcrossDatesAndBookingStates() {
        Booking confirmed = booking(propertyId, roomTypeId, FIRST_NIGHT, THIRD_NIGHT, 1, BookingStatus.CONFIRMED);
        Booking pending = booking(propertyId, roomTypeId, THIRD_NIGHT, FIFTH_NIGHT, 2, BookingStatus.PENDING_PAYMENT);
        Booking failed = booking(propertyId, roomTypeId, FIRST_NIGHT, FIFTH_NIGHT, 3, BookingStatus.PAYMENT_FAILED);
        Booking cancelled = booking(propertyId, roomTypeId, FIRST_NIGHT, FIFTH_NIGHT, 3, BookingStatus.CANCELLED);
        Booking differentProperty = booking(UUID.randomUUID(), roomTypeId, FIRST_NIGHT, FIFTH_NIGHT, 3, BookingStatus.CONFIRMED);
        Booking differentRoom = booking(propertyId, UUID.randomUUID(), FIRST_NIGHT, FIFTH_NIGHT, 3, BookingStatus.CONFIRMED);
        Booking outsideDates = booking(propertyId, roomTypeId, FIFTH_NIGHT, FIFTH_NIGHT.plusDays(1), 3, BookingStatus.CONFIRMED);
        List<Booking> bookings = List.of(
                confirmed, pending, failed, cancelled, differentProperty, differentRoom, outsideDates
        );

        InventoryAvailability availability = new InventoryAvailability();
        assertThat(availability.availableRooms(propertyId, roomType, FIRST_NIGHT, FIFTH_NIGHT, bookings))
                .isEqualTo(1);
        assertThat(availability.availableRooms(propertyId, roomType, THIRD_NIGHT, FIFTH_NIGHT, bookings))
                .isEqualTo(1);
    }

    @Test
    void roomAvailabilitySupportsBrowseAndDateSpecificChecks() {
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InventoryAvailability inventoryAvailability = new InventoryAvailability();
        RoomAvailabilitySearchFilter filter = new RoomAvailabilitySearchFilter(bookings, inventoryAvailability);

        assertThat(filter.matches(property, roomType,
                criteria(null, null, 5, null, null, null, Set.of()))).isTrue();
        assertThat(filter.matches(property, roomType,
                criteria(FIRST_NIGHT, THIRD_NIGHT, 2, null, null, null, Set.of()))).isTrue();
        bookings.save(booking(propertyId, roomTypeId, FIRST_NIGHT, THIRD_NIGHT, 3, BookingStatus.PENDING_PAYMENT));
        assertThat(filter.matches(property, roomType,
                criteria(FIRST_NIGHT, THIRD_NIGHT, 2, null, null, null, Set.of()))).isFalse();
        assertThat(filter.matches(property, roomType,
                criteria(FIRST_NIGHT, THIRD_NIGHT, 7, null, null, null, Set.of()))).isFalse();
    }

    private PropertySearchCriteria criteria(LocalDate checkIn, LocalDate checkOut, int guests,
                                            Long minimumPrice, Long maximumPrice, Integer minimumStars,
                                            Set<String> amenities) {
        return new PropertySearchCriteria(
                null, null, checkIn, checkOut, guests,
                minimumPrice, maximumPrice, minimumStars, amenities
        );
    }

    private PropertySearchCriteria locationCriteria(String city, String locality) {
        return new PropertySearchCriteria(city, locality, null, null, 1,
                null, null, null, Set.of());
    }

    private Booking booking(UUID bookingPropertyId, UUID bookingRoomTypeId, LocalDate checkIn,
                            LocalDate checkOut, int rooms, BookingStatus status) {
        return new Booking(
                UUID.randomUUID(), bookingPropertyId, bookingRoomTypeId, "Guest", "guest@example.com",
                checkIn, checkOut, 1, rooms, 1000L, status, java.time.Instant.now()
        );
    }
}
