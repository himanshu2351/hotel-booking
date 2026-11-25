package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyOnboardingValidatorTest {
    private final PropertyOnboardingValidator validator = new PropertyOnboardingValidator();

    @Test
    void acceptsValidOwnerAndPropertyDetails() {
        assertThatCode(() -> validator.validateOwner(new OwnerDetails("Owner", "owner@example.com")))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 5,
                List.of(room("Queen", 2, 3, 0L))))).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrIncompleteOwnerDetails() {
        assertBadRequest(() -> validator.validateOwner(null));
        assertBadRequest(() -> validator.validateOwner(new OwnerDetails(" ", "owner@example.com")));
        assertBadRequest(() -> validator.validateOwner(new OwnerDetails("Owner", null)));
        assertBadRequest(() -> validator.validateOwner(new OwnerDetails("Owner", " ")));
        assertBadRequest(() -> validator.validateOwner(new OwnerDetails("Owner", "invalid-email")));
        assertBadRequest(() -> validator.validateOwner(new OwnerDetails("Owner", "x".repeat(255) + "@a.co")));
    }

    @Test
    void rejectsMissingOrInvalidPropertyDetails() {
        assertBadRequest(() -> validator.validateProperty(null));
        assertBadRequest(() -> validator.validateProperty(property("", "Mumbai", "Colaba", 4, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "", "Colaba", 4, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", " ", 4, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 0, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 6, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, null)));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, List.of())));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, List.of(room(" ", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, List.of(room("Queen", 0, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, List.of(room("Queen", 2, 0, 0L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4, List.of(room("Queen", 2, 3, -1L)))));
        assertBadRequest(() -> validator.validateProperty(property("Hotel", "Mumbai", "Colaba", 4,
                java.util.Arrays.asList((PropertyDetails.RoomTypeDetails) null))));
        Set<String> amenitiesWithNull = new HashSet<>();
        amenitiesWithNull.add(null);
        assertBadRequest(() -> validator.validateProperty(new PropertyDetails("Hotel", "Mumbai", "Colaba", "",
                4, amenitiesWithNull, List.of(room("Queen", 2, 3, 0L)))));
        assertBadRequest(() -> validator.validateProperty(new PropertyDetails("Hotel", "Mumbai", "Colaba", "",
                4, Set.of(" "), List.of(room("Queen", 2, 3, 0L)))));
    }

    private PropertyDetails property(String name, String city, String locality, int rating,
                                     List<PropertyDetails.RoomTypeDetails> roomTypes) {
        return new PropertyDetails(name, city, locality, "", rating, Set.of(), roomTypes);
    }

    private PropertyDetails.RoomTypeDetails room(String name, int maxGuests, int inventory, long price) {
        return new PropertyDetails.RoomTypeDetails(name, maxGuests, inventory, price);
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BadRequestException.class);
    }
}
