package com.project.hotel_booking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.project.hotel_booking.exception.PaymentProcessingException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void mapsBadRequestToProblemDetail() {
        assertProblem(exceptionHandler.badRequest(new BadRequestException("invalid")), HttpStatus.BAD_REQUEST, "invalid");
    }

    @Test
    void mapsMissingResourceToProblemDetail() {
        assertProblem(exceptionHandler.notFound(new NotFoundException("missing")), HttpStatus.NOT_FOUND, "missing");
    }

    @Test
    void mapsConflictToProblemDetail() {
        assertProblem(exceptionHandler.conflict(new ConflictException("conflict")), HttpStatus.CONFLICT, "conflict");
    }

    @Test
    void mapsMalformedRequestsToBadRequestProblemDetail() {
        assertProblem(exceptionHandler.malformedRequest(new IllegalArgumentException()),
                HttpStatus.BAD_REQUEST, "Request is invalid or incomplete");
    }

    @Test
    void mapsPaymentProcessorFailuresToBadGatewayProblemDetail() {
        PaymentProcessingException exception = new PaymentProcessingException("Payment outcome unknown",
                new IllegalStateException("Gateway timeout"));
        assertProblem(exceptionHandler.paymentProcessingFailure(exception), HttpStatus.BAD_GATEWAY,
                "Payment outcome unknown");
    }

    private void assertProblem(ProblemDetail detail, HttpStatus status, String message) {
        assertThat(detail.getStatus()).isEqualTo(status.value());
        assertThat(detail.getTitle()).isEqualTo(status.getReasonPhrase());
        assertThat(detail.getDetail()).isEqualTo(message);
    }
}
