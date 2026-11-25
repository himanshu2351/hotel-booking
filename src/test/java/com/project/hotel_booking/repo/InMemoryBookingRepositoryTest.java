package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBookingRepositoryTest {
    private final InMemoryBookingRepository repository = new InMemoryBookingRepository();

    @Test
    void savesFindsAndListsBookings() {
        Booking booking = booking();

        assertThat(repository.save(booking)).isSameAs(booking);
        assertThat(repository.findById(booking.id())).contains(booking);
        assertThat(repository.findAll()).containsExactly(booking);
    }

    @Test
    void returnsEmptyResultsForUnknownBookings() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        assertThat(repository.findAll()).isEmpty();
    }

    private Booking booking() {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Guest",
                "guest@example.com", LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 2),
                1, 1, 5000L, BookingStatus.PENDING_PAYMENT, Instant.now());
    }
}
