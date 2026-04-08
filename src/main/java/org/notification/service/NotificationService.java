package org.notification.service;

import lombok.extern.slf4j.Slf4j;

import org.notification.model.Notification;
import org.notification.model.UserPreference;
import org.notification.model.enums.NotificationType;

import org.notification.repository.NotificationRepository;
import org.notification.repository.UserPreferenceRepository;

import org.notification.exception.NotificationNotFoundException;
import org.notification.exception.NotificationValidationException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    // ==============================
    // CONSTANTS
    // ==============================

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private static final String NOTIFICATION_NOT_FOUND =
            "Notification not found";

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";
    private static final String CHANNEL_IN_APP = "IN_APP";

    private final NotificationRepository repo;
    private final UserPreferenceRepository prefRepo;
    private final EmailService emailService;
    private final SmsService smsService;
    private final InAppService inAppService;

    // Constructor Injection
    public NotificationService(
            NotificationRepository repo,
            UserPreferenceRepository prefRepo,
            EmailService emailService,
            SmsService smsService,
            InAppService inAppService) {

        this.repo = repo;
        this.prefRepo = prefRepo;
        this.emailService = emailService;
        this.smsService = smsService;
        this.inAppService = inAppService;
    }

    // ==============================
    // CREATE NOTIFICATION
    // ==============================

    public Notification create(
            Notification n,
            String userId,
            String email) {

        validateNotification(n, userId);

        setDefaults(n, userId, email);

        checkUserPreferences(n, userId);

        Notification saved = repo.save(n);

        processChannel(saved);

        return repo.save(saved);
    }

    // ==============================
    // VALIDATION
    // ==============================

    private void validateNotification(
            Notification n,
            String userId) {

        if (userId == null) {

            throw new NotificationValidationException(
                    "UserId missing from JWT"
            );
        }

        if (n.getType() == null) {

            throw new NotificationValidationException(
                    "Notification type required"
            );
        }

        if (n.getChannel() == null) {

            throw new NotificationValidationException(
                    "Channel required"
            );
        }
    }

    // ==============================
    // DEFAULT VALUES
    // ==============================

    private void setDefaults(
            Notification n,
            String userId,
            String email) {

        n.setUserId(userId);
        n.setEmail(email);

        n.setNotificationId(
                UUID.randomUUID().toString()
        );

        n.setCreatedAt(
                System.currentTimeMillis()
        );

        n.setStatus(STATUS_PENDING);
        n.setRetryCount(0);

        if (n.getIsRead() == null)
            n.setIsRead(false);

        if (n.getIsScheduled() == null)
            n.setIsScheduled(false);
    }

    // ==============================
    // USER PREFERENCES
    // ==============================

    private void checkUserPreferences(
            Notification n,
            String userId) {

        UserPreference pref =
                prefRepo.getByUserId(userId);

        if (pref == null)
            return;

        NotificationType type =
                n.getType();

        switch (type) {

            case FEEDBACK_ALERT:

                validatePreference(
                        pref.getStudentFeedback(),
                        "FEEDBACK_ALERT disabled by user"
                );

                break;

            case SESSION_REMINDER:

                validatePreference(
                        pref.getLiveClassReminder(),
                        "SESSION_REMINDER disabled by user"
                );

                break;

            case PAYOUT_UPDATE:

                validatePreference(
                        pref.getPayoutUpdate(),
                        "PAYOUT_UPDATE disabled by user"
                );

                break;

            case STREAK_ALERT:

                validatePreference(
                        pref.getStreakUpdate(),
                        "STREAK_ALERT disabled by user"
                );

                break;

            case NEW_ENROLLMENT:

                validatePreference(
                        pref.getNewEnrollment(),
                        "NEW_ENROLLMENT disabled by user"
                );

                break;

            default:
                break;
        }
    }

    private void validatePreference(
            Boolean allowed,
            String message) {

        if (Boolean.FALSE.equals(allowed)) {

            throw new NotificationValidationException(
                    message
            );
        }
    }

    // ==============================
    // CHANNEL PROCESSING
    // ==============================

    private void processChannel(
            Notification saved) {

        try {

            String channel =
                    saved.getChannel();

            if (CHANNEL_IN_APP
                    .equalsIgnoreCase(channel)) {

                sendInApp(saved);

            } else if (CHANNEL_EMAIL
                    .equalsIgnoreCase(channel)) {

                sendEmail(saved);

            } else if (CHANNEL_SMS
                    .equalsIgnoreCase(channel)) {

                sendSms(saved);
            }

            saved.setStatus(STATUS_SENT);

        } catch (Exception e) {

            log.error(
                    "Notification sending failed id={}",
                    saved.getNotificationId(),
                    e
            );

            saved.setStatus(STATUS_FAILED);
        }
    }

    private void sendInApp(Notification n) {

        inAppService.createInApp(
                n.getUserId(),
                n.getTitle(),
                n.getDescription(),
                n.getType(),
                n.getRedirectUrl());
    }

    private void sendEmail(Notification n) {

        if (n.getEmail() == null) {

            throw new NotificationValidationException(
                    "Email required for EMAIL channel"
            );
        }

        emailService.sendEmail(
                n.getEmail(),
                n.getTitle(),
                n.getDescription());
    }

    private void sendSms(Notification n) {

        if (n.getPhoneNumber() == null) {

            throw new NotificationValidationException(
                    "Phone number required for SMS"
            );
        }

        smsService.sendSms(
                n.getPhoneNumber(),
                n.getDescription());
    }

    // ==============================
    // GET METHODS
    // ==============================

    public List<Notification> getByUser(
            String userId) {

        return repo.findByUserId(userId);
    }

    public Notification getById(String id) {

        return repo.findById(id)
                .orElseThrow(() ->
                        new NotificationNotFoundException(
                                NOTIFICATION_NOT_FOUND));
    }

    // ==============================
    // UPDATE
    // ==============================

    public Notification update(
            String id,
            Notification updated) {

        Notification existing =
                getById(id);

        updateFields(existing, updated);

        return repo.save(existing);
    }

    private void updateFields(
            Notification existing,
            Notification updated) {

        if (updated.getTitle() != null)
            existing.setTitle(updated.getTitle());

        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());

        if (updated.getStatus() != null)
            existing.setStatus(updated.getStatus());

        if (updated.getChannel() != null)
            existing.setChannel(updated.getChannel());

        if (updated.getPhoneNumber() != null)
            existing.setPhoneNumber(updated.getPhoneNumber());

        if (updated.getDeviceToken() != null)
            existing.setDeviceToken(updated.getDeviceToken());
    }

    // ==============================
    // DELETE
    // ==============================

    public void delete(String id) {

        Notification existing =
                getById(id);

        repo.delete(existing);
    }

}