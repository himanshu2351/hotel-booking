package com.project.hotel_booking.utils.validation;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentValidatorTest {
    private final PaymentValidator validator = new PaymentValidator();

    @Test
    void trimsValidIdempotencyKeyAndAcceptsSupportedMethod() {
        assertThat(validator.validateIdempotencyKey("  request-1  ")).isEqualTo("request-1");
        assertThat(validator.validateIdempotencyKey("k".repeat(200))).hasSize(200);
        assertThat(validator.validate(new PaymentRequest(PaymentMethod.CARD), Set.of(PaymentMethod.CARD)))
                .isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void rejectsMissingBlankOrOverlongIdempotencyKeys() {
        assertBadRequest(() -> validator.validateIdempotencyKey(null));
        assertBadRequest(() -> validator.validateIdempotencyKey("  "));
        assertBadRequest(() -> validator.validateIdempotencyKey("x".repeat(201)));
    }

    @Test
    void rejectsMissingOrUnsupportedPaymentMethods() {
        assertBadRequest(() -> validator.validate(null, Set.of(PaymentMethod.CARD)));
        assertBadRequest(() -> validator.validate(new PaymentRequest(null), Set.of(PaymentMethod.CARD)));
        assertBadRequest(() -> validator.validate(new PaymentRequest(PaymentMethod.UPI), Set.of(PaymentMethod.CARD)));
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BadRequestException.class);
    }
}
