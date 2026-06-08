package org.notification.exception;

public class PushSendingException extends RuntimeException {
    public PushSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
