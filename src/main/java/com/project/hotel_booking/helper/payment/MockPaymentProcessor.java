package com.project.hotel_booking.helper.payment;

import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentResult;
import org.springframework.stereotype.Component;
import java.util.UUID;

/** Local stand-in for a gateway. Swap this bean for a real adapter when integrating payments. */
@Component
public class MockPaymentProcessor implements PaymentProcessor {
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
        return new PaymentResult(
                true,
                "mock-" + UUID.randomUUID(),
                "Payment approved"
        );
    }
}
