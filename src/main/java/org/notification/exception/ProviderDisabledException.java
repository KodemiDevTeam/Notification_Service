package org.notification.exception;

public class ProviderDisabledException extends PermanentFailureException {
    public ProviderDisabledException(String message) {
        super(message);
    }
}
