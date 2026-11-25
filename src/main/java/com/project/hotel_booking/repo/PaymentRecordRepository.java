package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.payment.PaymentRecord;
import java.util.Optional;

public interface PaymentRecordRepository {
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
    PaymentRecord save(PaymentRecord paymentRecord);
}
