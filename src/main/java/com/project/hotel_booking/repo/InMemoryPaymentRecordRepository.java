package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.payment.PaymentRecord;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRecordRepository implements PaymentRecordRepository {
    private final ConcurrentHashMap<String, PaymentRecord> payments = new ConcurrentHashMap<>();

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(payments.get(idempotencyKey));
    }

    @Override
    public PaymentRecord save(PaymentRecord paymentRecord) {
        payments.put(paymentRecord.idempotencyKey(), paymentRecord);
        return paymentRecord;
    }
}
