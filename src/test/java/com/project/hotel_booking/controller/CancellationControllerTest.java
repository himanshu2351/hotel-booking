package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.cancellation.CancellationOutcome;
import com.project.hotel_booking.service.CancellationOperations;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancellationControllerTest {
    private final CancellationOperations cancellationService = mock(CancellationOperations.class);
    private final CancellationController controller = new CancellationController(cancellationService);

    @Test
    void delegatesCancellationForBooking() {
        UUID bookingId = UUID.randomUUID();
        CancellationOutcome expectedOutcome = mock(CancellationOutcome.class);
        when(cancellationService.cancel(bookingId)).thenReturn(expectedOutcome);

        assertThat(controller.cancel(bookingId)).isSameAs(expectedOutcome);
        verify(cancellationService).cancel(bookingId);
    }
}
