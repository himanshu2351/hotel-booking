package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.service.BookingOperations;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingControllerTest {
    private final BookingOperations bookingService = mock(BookingOperations.class);
    private final BookingController controller = new BookingController(bookingService);

    @Test
    void createsBookingByDelegatingRequestToService() {
        BookingRequest request = mock(BookingRequest.class);
        Booking expectedBooking = mock(Booking.class);
        when(bookingService.createBooking(request)).thenReturn(expectedBooking);

        Booking result = controller.book(request);

        assertThat(result).isSameAs(expectedBooking);
        verify(bookingService).createBooking(request);
    }

    @Test
    void retrievesBookingById() {
        UUID bookingId = UUID.randomUUID();
        Booking expectedBooking = mock(Booking.class);
        when(bookingService.booking(bookingId)).thenReturn(expectedBooking);

        assertThat(controller.booking(bookingId)).isSameAs(expectedBooking);
        verify(bookingService).booking(bookingId);
    }
}
