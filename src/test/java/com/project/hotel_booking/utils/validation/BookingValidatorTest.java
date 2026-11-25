package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.booking.BookingRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class BookingValidatorTest {
    private final BookingValidator validator = new BookingValidator();

    @Test
    void acceptsValidBookingAndSufficientGuestCapacity() {
        BookingRequest request = request(LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 2), 2, 1,
                "Guest", "guest@example.com");
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateGuestCapacity(2, 2, 4)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrInvalidDates() {
        assertBadRequest(() -> validator.validate(null));
        assertBadRequest(() -> validator.validate(request(null, LocalDate.now(), 1, 1, "Guest", "guest@example.com")));
        assertBadRequest(() -> validator.validate(request(LocalDate.now(), null, 1, 1, "Guest", "guest@example.com")));
        assertBadRequest(() -> validator.validate(request(LocalDate.of(2031, 1, 2), LocalDate.of(2031, 1, 1),
                1, 1, "Guest", "guest@example.com")));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertBadRequest(() -> validator.validate(request(yesterday, yesterday.plusDays(1),
                1, 1, "Guest", "guest@example.com")));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertBadRequest(() -> validator.validate(request(tomorrow, tomorrow, 1, 1, "Guest", "guest@example.com")));
    }

    @Test
    void rejectsInvalidGuestRoomCountsAndGuestDetails() {
        LocalDate start = LocalDate.of(2031, 1, 1);
        LocalDate end = start.plusDays(1);
        assertBadRequest(() -> validator.validate(request(start, end, 0, 1, "Guest", "guest@example.com")));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 0, "Guest", "guest@example.com")));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, " ", "guest@example.com")));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, "Guest", null)));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, "Guest", "not-an-email")));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, "Guest", "x".repeat(255) + "@a.co")));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, "Guest", "guest@example.com",
                null, UUID.randomUUID())));
        assertBadRequest(() -> validator.validate(request(start, end, 1, 1, "Guest", "guest@example.com",
                UUID.randomUUID(), null)));
        assertBadRequest(() -> validator.validateGuestCapacity(2, 1, 3));
        assertBadRequest(() -> validator.validateGuestCapacity(0, 1, 1));
        assertBadRequest(() -> validator.validateGuestCapacity(1, 0, 1));
        assertBadRequest(() -> validator.validateGuestCapacity(1, 1, 0));
        assertThatCode(() -> validator.validateGuestCapacity(1_000_000_000, 3, 2_000_000_000))
                .doesNotThrowAnyException();
    }

    private BookingRequest request(LocalDate checkIn, LocalDate checkOut, int guests, int rooms,
                                   String guestName, String guestEmail) {
        return request(checkIn, checkOut, guests, rooms, guestName, guestEmail, UUID.randomUUID(), UUID.randomUUID());
    }

    private BookingRequest request(LocalDate checkIn, LocalDate checkOut, int guests, int rooms,
                                   String guestName, String guestEmail, UUID propertyId, UUID roomTypeId) {
        return new BookingRequest(propertyId, roomTypeId, guestName, guestEmail,
                checkIn, checkOut, guests, rooms);
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BadRequestException.class);
    }
}
