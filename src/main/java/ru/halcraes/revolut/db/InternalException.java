package ru.halcraes.revolut.db;

/**
 * Represents an infrastructure or programming error.
 */
public class InternalException extends RuntimeException {
    public InternalException() {
    }

    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalException(Throwable cause) {
        super(cause);
    }
}
