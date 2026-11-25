package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PropertyOnboardingValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_EMAIL_LENGTH = 254;

    public void validateOwner(OwnerDetails owner) {
        if (owner == null) {
            throw new BadRequestException("Owner details are required");
        }

        requireText(owner.ownerName(), "Owner name is required");
        requireText(owner.ownerEmail(), "Owner email is required");
        validateEmail(owner.ownerEmail());
    }

    public void validateProperty(PropertyDetails details) {
        if (details == null) {
            throw new BadRequestException("Property details are required");
        }

        requireText(details.name(), "Property name is required");
        requireText(details.city(), "City is required");
        requireText(details.locality(), "Locality is required");

        if (details.starRating() < 1 || details.starRating() > 5) {
            throw new BadRequestException("Star rating must be between 1 and 5");
        }

        if (details.roomTypes() == null || details.roomTypes().isEmpty()) {
            throw new BadRequestException("At least one room type is required");
        }

        if (details.amenities() != null
                && details.amenities().stream().anyMatch(amenity -> amenity == null || amenity.isBlank())) {
            throw new BadRequestException("Amenities cannot be blank");
        }

        for (PropertyDetails.RoomTypeDetails room : details.roomTypes()) {
            if (room == null) {
                throw new BadRequestException("Room type details are required");
            }

            requireText(room.name(), "Room type name is required");
            if (room.maxGuests() < 1 || room.inventoryCount() < 1 || room.nightlyPriceMinor() < 0) {
                throw new BadRequestException("Room capacity and inventory must be positive; price cannot be negative");
            }
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void validateEmail(String email) {
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Owner email must be a valid email address");
        }
    }
}
