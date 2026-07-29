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
    private final FcmPushService fcmPushService;
    private final SseConnectionManager sseConnectionManager;

    public NotificationDispatcher(EmailSenderService emailService,
                                  SmsSenderService smsService,
                                  FcmPushService fcmPushService,
                                  SseConnectionManager sseConnectionManager) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.fcmPushService = fcmPushService;
        this.sseConnectionManager = sseConnectionManager;
    }

    public void dispatch(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL -> dispatchEmail(notification);
            case SMS -> dispatchSms(notification);
            case IN_APP -> dispatchInApp(notification);
            default -> throw new PermanentFailureException("Unknown channel: " + notification.getChannel());
        }
    }

    private void dispatchEmail(Notification notification) {
        if (!ProviderConfigValidator.isEmailEnabled()) {
            throw new ProviderDisabledException("EMAIL_PROVIDER_DISABLED");
        }
        if (isInvalid(notification.getRecipientEmail())) {
            throw new PermanentFailureException("Email address is missing or invalid");
        }
        try {
            emailService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getTitle(),
                    notification.getMessage()
            );
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("invalid")) {
                throw new PermanentFailureException("Invalid email address: " + e.getMessage(), e);
            }
            throw e; // Temporary failure
        }
    }

    private void dispatchSms(Notification notification) {
        if (!ProviderConfigValidator.isSmsEnabled()) {
            throw new ProviderDisabledException("SMS_PROVIDER_DISABLED");
        }
        if (isInvalid(notification.getRecipientPhone())) {
            throw new PermanentFailureException("Phone number is missing");
        }
        smsService.sendSms(
                notification.getRecipientPhone(),
                notification.getTitle() + ": " + notification.getMessage()
        );
    }

    private void dispatchInApp(Notification notification) {
        // 1. Send via SSE to Web browser (if connected)
        try {
            org.notification.dto.response.NotificationResponse payload =
                    org.notification.dto.response.NotificationResponse.fromEntity(notification);
            sseConnectionManager.sendNotification(notification.getUserId(), payload);
        } catch (Exception e) {
            log.warn("Failed to stream notification via SSE to user {}: {}", notification.getUserId(), e.getMessage());
        }

        // 2. Send via FCM to Mobile devices (if tokens registered)
        try {
            fcmPushService.sendPushNotification(
                    notification.getUserId(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getType() != null ? notification.getType().name() : null,
                    notification.getReferenceId()
            );
        } catch (Exception e) {
            log.warn("Failed to push notification via FCM to user {}: {}", notification.getUserId(), e.getMessage());
        }
    }

    private boolean isInvalid(String val) {
        return val == null || val.isBlank();
    }
}