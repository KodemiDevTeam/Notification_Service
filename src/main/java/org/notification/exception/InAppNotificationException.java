package org.notification.exception;

public class InAppNotificationException
        extends RuntimeException {

    public InAppNotificationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
