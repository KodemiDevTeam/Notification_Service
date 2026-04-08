package org.notification.exception;

public class InvalidEmailException extends RuntimeException {

    // Default constructor
    public InvalidEmailException() {
        super();
    }

    // Constructor with message
    public InvalidEmailException(String message) {
        super(message);
    }

    // Constructor with message and cause
    public InvalidEmailException(String message, Throwable cause) {
        super(message, cause);
    }

}
