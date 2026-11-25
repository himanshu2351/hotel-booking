package com.project.hotel_booking.helper.cancellation;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FullRefundCancellationPolicyTest {
    @Test
    void returnsTheFullBookingPriceAsRefund() {
        Booking booking = new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Guest",
                "guest@example.com", LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 2),
                1, 1, 12999L, BookingStatus.CONFIRMED, Instant.now());

        assertThat(new FullRefundCancellationPolicy().refundAmountMinor(booking)).isEqualTo(12999L);
    }
}
