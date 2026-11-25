package com.project.hotel_booking.helper.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockUpiPaymentProcessorTest {
    @Test
    void approvesUpiPaymentsWithMockTransactionId() {
        MockUpiPaymentProcessor processor = new MockUpiPaymentProcessor();
        PaymentResult result = processor.charge(UUID.randomUUID(), 5000L, "upi-key");

        assertThat(processor.method()).isEqualTo(PaymentMethod.UPI);
        assertThat(result.successful()).isTrue();
        assertThat(result.transactionId()).startsWith("mock-upi-");
        assertThat(result.message()).isEqualTo("Payment approved");
    }
}
