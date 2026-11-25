package com.project.hotel_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    ProblemDetail badRequest(BadRequestException exception) {
        return detail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    ProblemDetail malformedRequest(Exception exception) {
        return detail(HttpStatus.BAD_REQUEST, "Request is invalid or incomplete");
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception) {
        return detail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException exception) {
        return detail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PaymentProcessingException.class)
    ProblemDetail paymentProcessingFailure(PaymentProcessingException exception) {
        return detail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ProblemDetail detail(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return detail;
    }
}
