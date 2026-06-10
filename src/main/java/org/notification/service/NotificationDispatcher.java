package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.notification.config.ProviderConfigValidator;
import org.notification.exception.PermanentFailureException;
import org.notification.exception.ProviderDisabledException;
import org.notification.model.Notification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationDispatcher {

    private final EmailSenderService emailService;
    private final SmsSenderService smsService;

    public NotificationDispatcher(EmailSenderService emailService, SmsSenderService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void dispatch(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL:
                dispatchEmail(notification);
                break;
            case SMS:
                dispatchSms(notification);
                break;
            case IN_APP:
                // Stored in DB; nothing to push externally.
                break;
            default:
                throw new PermanentFailureException("Unknown channel: " + notification.getChannel());
        }
    }

    private void dispatchEmail(Notification notification) {
        if (!ProviderConfigValidator.isEmailEnabled()) {
            throw new ProviderDisabledException("EMAIL_PROVIDER_DISABLED");
        }
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new PermanentFailureException("Email address is missing or invalid");
        }
        try {
            emailService.sendEmail(notification.getRecipientEmail(), notification.getTitle(), notification.getMessage());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("invalid")) {
                throw new PermanentFailureException("Invalid email address: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    private void dispatchSms(Notification notification) {
        if (!ProviderConfigValidator.isSmsEnabled()) {
            throw new ProviderDisabledException("SMS_PROVIDER_DISABLED");
        }
        if (notification.getRecipientPhone() == null || notification.getRecipientPhone().isBlank()) {
            throw new PermanentFailureException("Phone number is missing");
        }
        smsService.sendSms(notification.getRecipientPhone(),
                notification.getTitle() + ": " + notification.getMessage());
    }
}
