package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentRecord;
import com.project.hotel_booking.helper.payment.PaymentResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPaymentRecordRepositoryTest {
    private final InMemoryPaymentRecordRepository repository = new InMemoryPaymentRecordRepository();

    @Test
    void savesAndFindsPaymentByIdempotencyKey() {
        PaymentRecord record = new PaymentRecord("key-1", UUID.randomUUID(), PaymentMethod.CARD,
                org.mockito.Mockito.mock(Booking.class), new PaymentResult(true, "txn-1", "approved"), Instant.now());

        assertThat(repository.save(record)).isSameAs(record);
        assertThat(repository.findByIdempotencyKey("key-1")).contains(record);
    }

    @Test
    void returnsEmptyForUnknownPaymentKey() {
        assertThat(repository.findByIdempotencyKey("missing")).isEmpty();
    }
}
