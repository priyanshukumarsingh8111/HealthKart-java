package com.healthkart.exception;

/**
 * Base abstract exception class for HealthKart application.
 * Demonstrates OOP Inheritance and Exception Handling hierarchy.
 */
public abstract class HealthKartException extends RuntimeException {
    private final String errorCode;

    public HealthKartException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public HealthKartException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
