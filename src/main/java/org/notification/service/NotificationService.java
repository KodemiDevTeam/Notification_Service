package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.notification.client.UserClient;
import org.notification.dto.request.BroadcastNotificationRequest;
import org.notification.dto.request.NotificationRequest;
import org.notification.dto.request.ScheduledNotificationRequest;
import org.notification.dto.response.BroadcastNotificationResponse;
import org.notification.dto.response.NotificationResponse;
import org.notification.dto.response.UserNotificationTargetDTO;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.SendMode;
import org.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final UserClient userClient;

    @Value("${notification.recurring.timezone:Asia/Kolkata}")
    private String defaultTimezone;

    @Value("${notification.recurring.morning-time:09:00}")
    private String defaultMorningTime;

    @Value("${notification.recurring.evening-time:18:00}")
    private String defaultEveningTime;

    @Value("${notification.retry.max-retries:3}")
    private int defaultMaxRetries;

    @Value("${internal.service.key:default-secret}")
    private String internalServiceKey;

    public NotificationService(NotificationRepository repository, UserClient userClient) {
        this.repository = repository;
        this.userClient = userClient;
    }

    public BroadcastNotificationResponse broadcast(BroadcastNotificationRequest req) {
        log.info("Starting broadcast for role: {}, sendMode: {}", req.getTargetRole(), req.getSendMode());
        List<UserNotificationTargetDTO> rawTargets = userClient.getUsersForNotification(req.getTargetRole());

        // Deduplicate targets by userId to prevent identical user objects from spamming
        List<UserNotificationTargetDTO> targets = new java.util.ArrayList<>();
        if (rawTargets != null) {
            java.util.Map<String, UserNotificationTargetDTO> targetMap = new java.util.LinkedHashMap<>();
            for (UserNotificationTargetDTO t : rawTargets) {
                if (t.getUserId() != null && !targetMap.containsKey(t.getUserId())) {
                    targetMap.put(t.getUserId(), t);
                }
            }
            targets.addAll(targetMap.values());
        }

        int totalUsers = targets.size();
        int inAppCreated = 0;
        int emailCreated = 0;
        int smsCreated = 0;
        int skippedEmailMissing = 0;
        int skippedPhoneMissing = 0;
        String batchId = UUID.randomUUID().toString();

        // Track emails and phones sent in this batch to prevent duplicates
        java.util.Set<String> processedEmails = new java.util.HashSet<>();
        java.util.Set<String> processedPhones = new java.util.HashSet<>();

        if (!targets.isEmpty()) {
            for (UserNotificationTargetDTO target : targets) {
                if (req.getChannels() == null) continue;

                for (NotificationChannel channel : req.getChannels()) {
                    try {
                        String email = target.getEmail() != null ? target.getEmail().trim().toLowerCase() : null;
                        String phone = target.getPhoneNumber() != null ? target.getPhoneNumber().trim() : null;

                        if (channel == NotificationChannel.EMAIL) {
                            if (email == null || email.isBlank()) {
                                skippedEmailMissing++;
                                continue;
                            }
                            if (!processedEmails.add(email)) {
                                continue; // Already sent an email to this address in this broadcast
                            }
                        }

                        if (channel == NotificationChannel.SMS) {
                            if (phone == null || phone.isBlank()) {
                                skippedPhoneMissing++;
                                continue;
                            }
                            if (!processedPhones.add(phone)) {
                                continue; // Already sent an SMS to this phone in this broadcast
                            }
                        }

                        int created = createRecordsForMode(batchId, target.getUserId(), email, phone, channel, req);

                        if (created > 0) {
                            if (channel == NotificationChannel.IN_APP) inAppCreated += created;
                            if (channel == NotificationChannel.EMAIL) emailCreated += created;
                            if (channel == NotificationChannel.SMS) smsCreated += created;
                        }
                    } catch (Exception e) {
                        log.error("Failed to create broadcast records for user {} channel {}", target.getUserId(), channel, e);
                    }
                }
            }
        }

        return BroadcastNotificationResponse.builder()
                .batchId(batchId)
                .totalUsers(totalUsers)
                .inAppCreated(inAppCreated)
                .emailCreated(emailCreated)
                .smsCreated(smsCreated)
                .totalNotificationsCreated(inAppCreated + emailCreated + smsCreated)
                .skippedEmailMissing(skippedEmailMissing)
                .skippedPhoneMissing(skippedPhoneMissing)
                .message("Broadcast enqueued successfully.")
                .build();
    }

    private int createRecordsForMode(String batchId, String userId, String email, String phone, NotificationChannel channel, BroadcastNotificationRequest req) {
        long now = System.currentTimeMillis();
        int created = 0;
        String tz = req.getTimezone() != null ? req.getTimezone() : defaultTimezone;
        ZoneId zoneId = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zoneId);

        if (req.getSendMode() == SendMode.SEND_NOW) {
            saveRecord(batchId, userId, email, phone, channel, req, now, null, null, null);
            created++;
        } else if (req.getSendMode() == SendMode.SCHEDULED) {
            long scheduledAtEpoch = now;
            if (req.getScheduledAt() != null && !req.getScheduledAt().isBlank()) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(req.getScheduledAt());
                    scheduledAtEpoch = ldt.atZone(zoneId).toInstant().toEpochMilli();
                } catch (Exception e) {
                    log.error("Failed to parse scheduledAt: {}", req.getScheduledAt());
                }
            }
            saveRecord(batchId, userId, email, phone, channel, req, scheduledAtEpoch, null, null, null);
            created++;
        } else if (req.getSendMode() == SendMode.DAILY_MORNING) {
            created += tryCreateRecurring(batchId, userId, email, phone, channel, req, today, "MORNING", req.getMorningTime(), defaultMorningTime, zoneId);
        } else if (req.getSendMode() == SendMode.DAILY_EVENING) {
            created += tryCreateRecurring(batchId, userId, email, phone, channel, req, today, "EVENING", req.getEveningTime(), defaultEveningTime, zoneId);
        } else if (req.getSendMode() == SendMode.DAILY_MORNING_EVENING) {
            created += tryCreateRecurring(batchId, userId, email, phone, channel, req, today, "MORNING", req.getMorningTime(), defaultMorningTime, zoneId);
            created += tryCreateRecurring(batchId, userId, email, phone, channel, req, today, "EVENING", req.getEveningTime(), defaultEveningTime, zoneId);
        } else if (req.getSendMode() == SendMode.CUSTOM_RECURRING) {
            long scheduledAtEpoch = now;
            if (req.getScheduledAt() != null && !req.getScheduledAt().isBlank()) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(req.getScheduledAt());
                    scheduledAtEpoch = ldt.atZone(zoneId).toInstant().toEpochMilli();
                } catch (Exception e) {
                    log.error("Failed to parse scheduledAt: {}", req.getScheduledAt());
                }
            }
            saveRecord(batchId, userId, email, phone, channel, req, scheduledAtEpoch, null, null, null);
            created++;
        }

        return created;
    }

    private int tryCreateRecurring(String batchId, String userId, String email, String phone, NotificationChannel channel,
                                   BroadcastNotificationRequest req, LocalDate date, String slot, String reqTime, String defTime, ZoneId zone) {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String recurrenceKey = String.format("%s_%s_%s_%s_%s", batchId, userId, channel.name(), dateStr, slot);

        if (repository.existsByRecurrenceKey(recurrenceKey)) {
            return 0; // Prevent duplicate
        }

        String timeStr = (reqTime != null && !reqTime.isBlank()) ? reqTime : defTime;
        LocalTime time = LocalTime.parse(timeStr);
        ZonedDateTime zdt = ZonedDateTime.of(date, time, zone);
        long scheduledAt = zdt.toInstant().toEpochMilli();

        saveRecord(batchId, userId, email, phone, channel, req, scheduledAt, recurrenceKey, dateStr, slot);
        return 1;
    }

    private void saveRecord(String batchId, String userId, String email, String phone, NotificationChannel channel,
                            BroadcastNotificationRequest req, long scheduledAt, String recurrenceKey, String recurrenceDate, String recurrenceSlot) {
        Notification n = buildNotification(userId, email, phone, req.getTitle(), req.getMessage(), req.getType(), channel, req.getRedirectUrl(), req.getReferenceId(), req.getPriority(), batchId, req.getSendMode(), scheduledAt, req.getMaxRetries(), recurrenceKey, recurrenceDate, recurrenceSlot);
        if (!repository.saveIdempotent(n)) {
            log.info("Duplicate broadcast notification skipped for user {} channel {}", userId, channel);
        }
    }

    private Notification buildNotification(String userId, String email, String phone, String title, String message,
                                           org.notification.model.enums.NotificationType type,
                                           org.notification.model.enums.NotificationChannel channel,
                                           String redirectUrl, String referenceId,
                                           org.notification.model.enums.NotificationPriority priority,
                                           String batchId, org.notification.model.enums.SendMode sendMode, long scheduledAt,
                                           Integer maxRetries, String recurrenceKey, String recurrenceDate, String recurrenceSlot) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setRecipientEmail(email);
        n.setRecipientPhone(phone);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setChannel(channel);
        n.setRedirectUrl(redirectUrl);
        n.setReferenceId(referenceId);
        n.setBatchId(batchId);

        String idempotencyKey = type.name() + "_" + userId + "_" + (referenceId != null ? referenceId : "NONE") + "_" + channel.name();
        if (recurrenceKey != null) {
            idempotencyKey += "_" + recurrenceKey;
        } else if (batchId != null) {
            idempotencyKey += "_" + batchId;
        }
        n.setIdempotencyKey(idempotencyKey);
        n.setNotificationId(UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8)).toString());

        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(sendMode);
        n.setPriority(priority != null ? priority : org.notification.model.enums.NotificationPriority.NORMAL);
        n.setScheduledAt(scheduledAt);

        long now = System.currentTimeMillis();
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        n.setExpirationTime((now / 1000) + (30L * 24 * 60 * 60)); // +30 days TTL in seconds

        n.setMaxRetries(maxRetries != null ? maxRetries : defaultMaxRetries);
        n.setRetryCount(0);

        if (recurrenceKey != null) {
            n.setRecurrenceKey(recurrenceKey);
            n.setRecurrenceDate(recurrenceDate);
            n.setRecurrenceSlot(recurrenceSlot);
        }

        return n;
    }

    public void cancelBatch(String batchId) {
        List<Notification> records = repository.findByBatchId(batchId);
        for (Notification n : records) {
            if (n.getStatus() == NotificationStatus.PENDING || n.getStatus() == NotificationStatus.RETRY_SCHEDULED) {
                n.setStatus(NotificationStatus.CANCELLED);
                n.setUpdatedAt(System.currentTimeMillis());
                repository.save(n);
            }
        }
    }

    public void sendNowBatch(String batchId) {
        List<Notification> records = repository.findByBatchId(batchId);
        long now = System.currentTimeMillis();
        for (Notification n : records) {
            if (n.getStatus() == NotificationStatus.PENDING || n.getStatus() == NotificationStatus.RETRY_SCHEDULED) {
                n.setScheduledAt(now);
                n.setUpdatedAt(System.currentTimeMillis());
                repository.save(n);
            }
        }
    }

    public void rescheduleBatch(String batchId, Long newScheduledAt) {
        if (newScheduledAt == null) return;
        List<Notification> records = repository.findByBatchId(batchId);
        for (Notification n : records) {
            if (n.getStatus() == NotificationStatus.PENDING || n.getStatus() == NotificationStatus.RETRY_SCHEDULED) {
                n.setScheduledAt(newScheduledAt);
                n.setUpdatedAt(System.currentTimeMillis());
                repository.save(n);
            }
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#req.userId"),
            @CacheEvict(value = "unreadCount", key = "#req.userId")
    })
    public void sendImmediate(NotificationRequest req) {
        Notification n = buildNotification(req.getUserId(), req.getEmail(), req.getPhoneNumber(), req.getTitle(), req.getMessage(), req.getType(), req.getChannel(), req.getRedirectUrl(), req.getReferenceId(), req.getPriority(), null, SendMode.SEND_NOW, System.currentTimeMillis(), defaultMaxRetries, null, null, null);
        if (!repository.saveIdempotent(n)) {
            log.info("Duplicate immediate notification skipped for user {} channel {}", req.getUserId(), req.getChannel());
        }
    }


    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#req.userId"),
            @CacheEvict(value = "unreadCount", key = "#req.userId")
    })
    public void sendInternal(NotificationRequest req) {
        ensureChannelsPresent(req);
        populateContactDetailsIfNeeded(req);

        for (NotificationChannel channel : req.getChannels()) {
            processChannelNotification(req, channel);
        }
    }

    private void ensureChannelsPresent(NotificationRequest req) {
        if (req.getChannels() != null && !req.getChannels().isEmpty()) {
            return;
        }
        NotificationChannel defaultChannel = (req.getChannel() != null)
                ? req.getChannel()
                : NotificationChannel.IN_APP;
        req.setChannels(java.util.Collections.singletonList(defaultChannel));
    }

    private void populateContactDetailsIfNeeded(NotificationRequest req) {
        if (!isContactInfoMissingForChannels(req)) {
            return;
        }

        try {
            UserNotificationTargetDTO contact = userClient.getUserContact(req.getUserId(), internalServiceKey);
            if (contact != null) {
                if (isBlank(req.getEmail())) {
                    req.setEmail(contact.getEmail());
                }
                if (isBlank(req.getPhoneNumber())) {
                    req.setPhoneNumber(contact.getPhoneNumber());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch contact details for user {}: {}", req.getUserId(), e.getMessage());
        }
    }

    private boolean isContactInfoMissingForChannels(NotificationRequest req) {
        for (NotificationChannel channel : req.getChannels()) {
            if (channel == NotificationChannel.EMAIL && isBlank(req.getEmail())) return true;
            if (channel == NotificationChannel.SMS && isBlank(req.getPhoneNumber())) return true;
        }
        return false;
    }

    private void processChannelNotification(NotificationRequest req, NotificationChannel channel) {
        try {
            if (shouldSkipChannel(req, channel)) {
                return;
            }

            Notification notification = buildNotification(
                    req.getUserId(), req.getEmail(), req.getPhoneNumber(),
                    req.getTitle(), req.getMessage(), req.getType(), channel,
                    req.getRedirectUrl(), req.getReferenceId(), req.getPriority(),
                    null, SendMode.SEND_NOW, System.currentTimeMillis(),
                    defaultMaxRetries, null, null, null
            );

            if (repository.saveIdempotent(notification)) {
                logChannelSuccess(req.getUserId(), channel);
            } else {
                log.info("Duplicate internal notification skipped for user {} channel {}", req.getUserId(), channel);
            }
        } catch (Exception e) {
            log.error("Failed to process channel {} for user {}", channel, req.getUserId(), e);
        }
    }

    private boolean shouldSkipChannel(NotificationRequest req, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL && isBlank(req.getEmail())) {
            log.warn("Skipping EMAIL for user {} due to missing email address.", req.getUserId());
            return true;
        }
        if (channel == NotificationChannel.SMS && isBlank(req.getPhoneNumber())) {
            log.warn("Skipping SMS for user {} due to missing phone number.", req.getUserId());
            return true;
        }
        return false;
    }

    private void logChannelSuccess(String userId, NotificationChannel channel) {
        switch (channel) {
            case IN_APP -> log.info("IN_APP notification created for user {}", userId);
            case EMAIL -> log.info("EMAIL notification queued for user {}", userId);
            case SMS -> log.info("SMS notification queued for user {}", userId);
            default -> log.info("{} notification processed for user {}", channel, userId);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ==========================================
    // Remaining Service Methods
    // ==========================================

    public void schedule(ScheduledNotificationRequest req) {
        Notification n = new Notification();
        n.setUserId(req.getUserId());
        n.setTitle(req.getTitle());
        n.setMessage(req.getDescription());
        n.setType(req.getType());
        n.setChannel(req.getChannel());
        n.setRecipientEmail(req.getEmail());
        n.setRecipientPhone(req.getPhoneNumber());
        n.setRedirectUrl(req.getRedirectUrl());
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SCHEDULED);
        n.setScheduledAt(req.getScheduledTime());
        n.setCreatedAt(System.currentTimeMillis());

        repository.save(n);
    }

    @Cacheable(value = "userNotifications", key = "#userId")
    public List<NotificationResponse> getUserNotifications(String userId) {
        List<Notification> entities = repository.findByUserId(userId);
        List<NotificationResponse> responses = new java.util.ArrayList<>();
        for (Notification n : entities) {
            responses.add(NotificationResponse.fromEntity(n));
        }
        return responses;
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", allEntries = true),
            @CacheEvict(value = "unreadCount", allEntries = true)
    })
    public void markAsRead(String notificationId) {
        Notification n = repository.findById(notificationId);
        if (n != null && !Boolean.TRUE.equals(n.getRead())) {
            n.setRead(true);
            repository.save(n);
        }
    }

    @Cacheable(value = "unreadCount", key = "#userId")
    public long getUnreadCount(String userId) {
        List<Notification> entities = repository.findByUserId(userId);
        long count = 0;
        for (Notification n : entities) {
            if (!Boolean.TRUE.equals(n.getRead())) {
                count++;
            }
        }
        return count;
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId"),
            @CacheEvict(value = "unreadCount", key = "#userId")
    })
    public void markAllAsRead(String userId) {
        List<Notification> notifications = repository.findByUserId(userId);
        for (Notification n : notifications) {
            if (!Boolean.TRUE.equals(n.getRead())) {
                n.setRead(true);
                repository.save(n);
            }
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", allEntries = true),
            @CacheEvict(value = "unreadCount", allEntries = true)
    })
    public void deleteNotification(String notificationId) {
        Notification notification = repository.findById(notificationId);
        if (notification != null) {
            repository.delete(notification);
            log.info("Deleted notification: {}", notificationId);
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", allEntries = true),
            @CacheEvict(value = "unreadCount", allEntries = true)
    })
    public void clearUserNotifications(String userId) {
        List<Notification> notifications = repository.findByUserId(userId);
        for (Notification notification : notifications) {
            repository.delete(notification);
        }
        log.info("Cleared {} notifications for user: {}", notifications.size(), userId);
    }
}