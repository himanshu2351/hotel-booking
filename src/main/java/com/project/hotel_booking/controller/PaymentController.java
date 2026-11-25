package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.payment.PaymentRequest;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.service.PaymentOperations;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payments")
public class PaymentController {
    private final PaymentOperations paymentService;
    public PaymentController(PaymentOperations paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentOutcome pay(
            @PathVariable UUID bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request
    ) {
        return paymentService.pay(bookingId, request, idempotencyKey);
    }
}
