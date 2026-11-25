package com.project.hotel_booking.helper.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockWalletPaymentProcessorTest {
    @Test
    void approvesWalletPaymentsWithMockTransactionId() {
        MockWalletPaymentProcessor processor = new MockWalletPaymentProcessor();
        PaymentResult result = processor.charge(UUID.randomUUID(), 5000L, "wallet-key");

        assertThat(processor.method()).isEqualTo(PaymentMethod.WALLET);
        assertThat(result.successful()).isTrue();
        assertThat(result.transactionId()).startsWith("mock-wallet-");
        assertThat(result.message()).isEqualTo("Payment approved");
    }
}
