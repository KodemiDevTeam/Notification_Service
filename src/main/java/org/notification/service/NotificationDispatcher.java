package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.notification.model.Notification;
import org.notification.exception.PermanentFailureException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationDispatcher {

    private final EmailSenderService emailService;
    private final SmsSenderService smsService;

    public NotificationDispatcher(EmailSenderService emailService,
                                  SmsSenderService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void dispatch(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL:
                if (!org.notification.config.ProviderConfigValidator.isEmailEnabled()) {
                    throw new org.notification.exception.ProviderDisabledException("EMAIL_PROVIDER_DISABLED");
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
                    throw e; // Temporary failure
                }
                break;
            case SMS:
                if (!org.notification.config.ProviderConfigValidator.isSmsEnabled()) {
                    throw new org.notification.exception.ProviderDisabledException("SMS_PROVIDER_DISABLED");
                }
                if (notification.getRecipientPhone() == null || notification.getRecipientPhone().isBlank()) {
                    throw new PermanentFailureException("Phone number is missing");
                }
                smsService.sendSms(notification.getRecipientPhone(), notification.getTitle() + ": " + notification.getMessage());
                break;
            case IN_APP:
                // No external sending. Notification remains in DB with status SENT.
                break;
            default:
                throw new PermanentFailureException("Unknown channel: " + notification.getChannel());
        }
    }
}
