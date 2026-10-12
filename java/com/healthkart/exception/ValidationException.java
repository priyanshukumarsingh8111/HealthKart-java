package com.healthkart.exception;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends HealthKartException {
    public ValidationException(String message) {
        super(message, "VALIDATION_FAILED");
    }
}
