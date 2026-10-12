package com.healthkart.model;

import java.io.Serializable;
import java.util.Optional;

/**
 * Generic response wrapper demonstrating Generics in Java.
 * Provides type-safe containment for data results and error states.
 *
 * @param <T> The payload type
 */
public class DataResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final T data;
    private final long timestamp;

    private DataResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> DataResult<T> success(T data) {
        return new DataResult<>(true, "Operation successful", data);
    }

    public static <T> DataResult<T> success(String message, T data) {
        return new DataResult<>(true, message, data);
    }

    public static <T> DataResult<T> error(String message) {
        return new DataResult<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Optional<T> optionalData() {
        return Optional.ofNullable(data);
    }

    public long getTimestamp() {
        return timestamp;
    }
}
