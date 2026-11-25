package com.project.hotel_booking.helper.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentProcessorTest {
    @Test
    void approvesCardPaymentsWithMockTransactionId() {
        MockPaymentProcessor processor = new MockPaymentProcessor();
        PaymentResult result = processor.charge(UUID.randomUUID(), 5000L, "card-key");

        assertThat(processor.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.successful()).isTrue();
        assertThat(result.transactionId()).startsWith("mock-");
        assertThat(result.message()).isEqualTo("Payment approved");
    }
}
