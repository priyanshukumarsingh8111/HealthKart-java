package com.healthkart.exception;

/**
 * Exception thrown when a database or JDBC operation fails.
 * Wraps java.sql.SQLException with additional context.
 */
public class DatabaseException extends HealthKartException {
    private final String sql;

    public DatabaseException(String message, Throwable cause) {
        super(message, "DB_ERROR", cause);
        this.sql = "";
    }

    public DatabaseException(String message, String sql, Throwable cause) {
        super(message + (sql != null && !sql.isEmpty() ? " [SQL: " + sql + "]" : ""), "DB_ERROR", cause);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
