package org.notification.exception;

public class SmsSendingException extends RuntimeException {

    public SmsSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
