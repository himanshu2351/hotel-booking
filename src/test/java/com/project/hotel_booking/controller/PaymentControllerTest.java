package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.payment.PaymentRequest;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.service.PaymentOperations;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {
    private final PaymentOperations paymentService = mock(PaymentOperations.class);
    private final PaymentController controller = new PaymentController(paymentService);

    @Test
    void forwardsBookingIdRequestAndIdempotencyKey() {
        UUID bookingId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(null);
        String idempotencyKey = "payment-attempt-1";
        PaymentOutcome expectedOutcome = mock(PaymentOutcome.class);
        when(paymentService.pay(bookingId, request, idempotencyKey)).thenReturn(expectedOutcome);

        assertThat(controller.pay(bookingId, idempotencyKey, request)).isSameAs(expectedOutcome);
        verify(paymentService).pay(bookingId, request, idempotencyKey);
    }
}
