package org.notification.exception;

public class NotificationValidationException
        extends RuntimeException {

    public NotificationValidationException(
            String message) {

        super(message);
    }
}