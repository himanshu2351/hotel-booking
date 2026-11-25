package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentRequest;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class PaymentValidator {
    public String validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }

        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > 200) {
            throw new BadRequestException("Idempotency-Key must not exceed 200 characters");
        }

        return normalizedKey;
    }

    public PaymentMethod validate(PaymentRequest request, Set<PaymentMethod> supportedMethods) {
        if (request == null) {
            throw new BadRequestException("Payment details are required");
        }

        PaymentMethod method = request.method();
        if (method == null) {
            throw new BadRequestException("Payment method is required");
        }

        if (!supportedMethods.contains(method)) {
            throw new BadRequestException("Unsupported payment method");
        }

        return method;
    }
}
