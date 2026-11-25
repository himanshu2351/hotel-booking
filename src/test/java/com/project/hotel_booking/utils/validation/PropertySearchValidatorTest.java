package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertySearchValidatorTest {
    private final PropertySearchValidator validator = new PropertySearchValidator();

    @Test
    void acceptsValidOptionalAndCompleteCriteria() {
        assertThatCode(() -> validator.validate(criteria(null, null, 1, null, null))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(criteria(null, null, 1, 100L, null))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(criteria(null, null, 1, null, 200L))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(criteria(null, null, 2, 100L, 200L))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(criteria(LocalDate.now(), LocalDate.now().plusDays(1),
                2, 100L, 200L))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(criteria(LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 2),
                2, 100L, 200L))).doesNotThrowAnyException();
    }

    @Test
    void treatsOmittedAmenitiesAsAnEmptyFilterSet() {
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                null, null, null, null, 1, null, null, null, null);

        assertThatCode(() -> validator.validate(criteria)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingMismatchedOrInvalidDates() {
        assertBadRequest(() -> validator.validate(null));
        assertBadRequest(() -> validator.validate(criteria(LocalDate.now(), null, 1, null, null)));
        assertBadRequest(() -> validator.validate(criteria(null, LocalDate.now(), 1, null, null)));
        assertBadRequest(() -> validator.validate(criteria(LocalDate.of(2031, 1, 2), LocalDate.of(2031, 1, 1),
                1, null, null)));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertBadRequest(() -> validator.validate(criteria(yesterday, yesterday.plusDays(1), 1, null, null)));
        assertBadRequest(() -> validator.validate(criteria(LocalDate.now(), LocalDate.now(), 1, null, null)));
    }

    @Test
    void rejectsInvalidGuestCountsAndPriceRanges() {
        assertBadRequest(() -> validator.validate(criteria(null, null, 0, null, null)));
        assertBadRequest(() -> validator.validate(criteria(null, null, 1, -1L, null)));
        assertBadRequest(() -> validator.validate(criteria(null, null, 1, null, -1L)));
        assertBadRequest(() -> validator.validate(criteria(null, null, 1, 900L, 800L)));
        assertBadRequest(() -> validator.validate(new PropertySearchCriteria(null, null, null, null,
                1, null, null, 0, Set.of())));
        assertBadRequest(() -> validator.validate(new PropertySearchCriteria(null, null, null, null,
                1, null, null, 6, Set.of())));
        assertBadRequest(() -> validator.validate(new PropertySearchCriteria(null, null, null, null,
                1, null, null, null, Set.of(" "))));
        Set<String> amenitiesWithNull = new HashSet<>();
        amenitiesWithNull.add(null);
        assertBadRequest(() -> validator.validate(new PropertySearchCriteria(null, null, null, null,
                1, null, null, null, amenitiesWithNull)));
    }

    private PropertySearchCriteria criteria(LocalDate checkIn, LocalDate checkOut, int guests,
                                            Long minimum, Long maximum) {
        return new PropertySearchCriteria(null, null, checkIn, checkOut, guests, minimum, maximum, null, Set.of());
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BadRequestException.class);
    }
}
