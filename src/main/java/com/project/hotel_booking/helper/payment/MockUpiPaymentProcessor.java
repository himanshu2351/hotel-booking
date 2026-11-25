package com.project.hotel_booking.helper.payment;

import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentResult;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class MockUpiPaymentProcessor implements PaymentProcessor {
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
        return new PaymentResult(
                true,
                "mock-upi-" + UUID.randomUUID(),
                "Payment approved"
        );
    }
}
