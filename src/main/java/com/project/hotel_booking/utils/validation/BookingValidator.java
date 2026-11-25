package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.booking.BookingRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Component
public class BookingValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_EMAIL_LENGTH = 254;

    public void validate(BookingRequest request) {
        if (request == null) {
            throw new BadRequestException("Booking details are required");
        }

        validateDates(request.checkIn(), request.checkOut());

        if (request.propertyId() == null || request.roomTypeId() == null) {
            throw new BadRequestException("Property and room type are required");
        }

        if (request.guests() < 1 || request.rooms() < 1) {
            throw new BadRequestException("Guests and rooms must be at least one");
        }

        requireText(request.guestName(), "Guest name is required");
        requireText(request.guestEmail(), "Guest email is required");
        validateEmail(request.guestEmail());
    }

    public void validateGuestCapacity(int maximumGuests, int roomCount, int guestCount) {
        if (maximumGuests < 1 || roomCount < 1 || guestCount < 1
                || (long) maximumGuests * roomCount < guestCount) {
            throw new BadRequestException("Selected rooms cannot accommodate the guests");
        }
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("Check-in and check-out are required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out must be after check-in");
        }

        if (checkIn.isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in cannot be in the past");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void validateEmail(String email) {
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Guest email must be a valid email address");
        }
    }
}
