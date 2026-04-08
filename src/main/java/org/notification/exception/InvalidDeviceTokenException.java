package org.notification.exception;

public class InvalidDeviceTokenException extends RuntimeException {

    public InvalidDeviceTokenException() {
        super();
    }

    public InvalidDeviceTokenException(String message) {
        super(message);
    }

    public InvalidDeviceTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}