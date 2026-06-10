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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

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

    public NotificationService(NotificationRepository repository, UserClient userClient) {
        this.repository = repository;
        this.userClient = userClient;
    }

    public BroadcastNotificationResponse broadcast(BroadcastNotificationRequest req) {
        log.info("Starting broadcast for role: {}, sendMode: {}", req.getTargetRole(), req.getSendMode());
        List<UserNotificationTargetDTO> targets = deduplicateTargets(userClient.getUsersForNotification(req.getTargetRole()));

        BroadcastCounts counts = new BroadcastCounts();
        String batchId = UUID.randomUUID().toString();

        if (!targets.isEmpty() && req.getChannels() != null) {
            java.util.Set<String> processedEmails = new java.util.HashSet<>();
            java.util.Set<String> processedPhones = new java.util.HashSet<>();
            for (UserNotificationTargetDTO target : targets) {
                processTarget(batchId, target, req, counts, processedEmails, processedPhones);
            }
        }

        return BroadcastNotificationResponse.builder()
                .batchId(batchId)
                .totalUsers(targets.size())
                .inAppCreated(counts.inApp)
                .emailCreated(counts.email)
                .smsCreated(counts.sms)
                .totalNotificationsCreated(counts.inApp + counts.email + counts.sms)
                .skippedEmailMissing(counts.skippedEmail)
                .skippedPhoneMissing(counts.skippedPhone)
                .message("Broadcast enqueued successfully.")
                .build();
    }

    private static List<UserNotificationTargetDTO> deduplicateTargets(List<UserNotificationTargetDTO> raw) {
        if (raw == null) return new java.util.ArrayList<>();
        java.util.Map<String, UserNotificationTargetDTO> map = new java.util.LinkedHashMap<>();
        for (UserNotificationTargetDTO t : raw) {
            if (t.getUserId() != null) map.putIfAbsent(t.getUserId(), t);
        }
        return new java.util.ArrayList<>(map.values());
    }

    private void processTarget(String batchId, UserNotificationTargetDTO target,
                               BroadcastNotificationRequest req, BroadcastCounts counts,
                               java.util.Set<String> processedEmails, java.util.Set<String> processedPhones) {
        for (NotificationChannel channel : req.getChannels()) {
            try {
                String email = target.getEmail() != null ? target.getEmail().trim().toLowerCase() : null;
                String phone = target.getPhoneNumber() != null ? target.getPhoneNumber().trim() : null;

                if (!canSendOnChannel(channel, email, phone, counts, processedEmails, processedPhones)) continue;

                int created = createRecordsForMode(batchId, target.getUserId(), email, phone, channel, req);
                counts.add(channel, created);
            } catch (Exception e) {
                log.error("Failed to create broadcast records for user {} channel {}", target.getUserId(), channel, e);
            }
        }
    }

    private static boolean canSendOnChannel(NotificationChannel channel, String email, String phone,
                                            BroadcastCounts counts,
                                            java.util.Set<String> processedEmails,
                                            java.util.Set<String> processedPhones) {
        if (channel == NotificationChannel.EMAIL) {
            if (email == null || email.isBlank()) { counts.skippedEmail++; return false; }
            return processedEmails.add(email);
        }
        if (channel == NotificationChannel.SMS) {
            if (phone == null || phone.isBlank()) { counts.skippedPhone++; return false; }
            return processedPhones.add(phone);
        }
        return true;
    }

    /** Mutable accumulator for broadcast counters — keeps broadcast() under the complexity limit. */
    private static final class BroadcastCounts {
        int inApp;
        int email;
        int sms;
        int skippedEmail;
        int skippedPhone;

        void add(NotificationChannel channel, int n) {
            if (channel == NotificationChannel.IN_APP) inApp += n;
            else if (channel == NotificationChannel.EMAIL) email += n;
            else if (channel == NotificationChannel.SMS) sms += n;
        }
    }

    private int createRecordsForMode(String batchId, String userId, String email, String phone,
                                     NotificationChannel channel, BroadcastNotificationRequest req) {
        long now = System.currentTimeMillis();
        String tz = req.getTimezone() != null ? req.getTimezone() : defaultTimezone;
        ZoneId zoneId = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zoneId);

        switch (req.getSendMode()) {
            case SEND_NOW:
                saveRecord(batchId, userId, email, phone, channel, req, now, null, null, null);
                return 1;
            case SCHEDULED:
                saveRecord(batchId, userId, email, phone, channel, req,
                        parseEpoch(req.getScheduledAt(), now, zoneId), null, null, null);
                return 1;
            case DAILY_MORNING:
                return tryCreateRecurring(batchId, userId, email, phone, channel, req,
                        today, "MORNING", req.getMorningTime(), defaultMorningTime, zoneId);
            case DAILY_EVENING:
                return tryCreateRecurring(batchId, userId, email, phone, channel, req,
                        today, "EVENING", req.getEveningTime(), defaultEveningTime, zoneId);
            case DAILY_MORNING_EVENING:
                return tryCreateRecurring(batchId, userId, email, phone, channel, req,
                        today, "MORNING", req.getMorningTime(), defaultMorningTime, zoneId)
                     + tryCreateRecurring(batchId, userId, email, phone, channel, req,
                        today, "EVENING", req.getEveningTime(), defaultEveningTime, zoneId);
            case CUSTOM_RECURRING:
                saveRecord(batchId, userId, email, phone, channel, req,
                        parseEpoch(req.getScheduledAt(), now, zoneId), null, null, null);
                return 1;
            default:
                return 0;
        }
    }

    private long parseEpoch(String scheduledAt, long fallback, ZoneId zoneId) {
        if (scheduledAt == null || scheduledAt.isBlank()) return fallback;
        try {
            return LocalDateTime.parse(scheduledAt).atZone(zoneId).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.error("Failed to parse scheduledAt: {}", scheduledAt);
            return fallback;
        }
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

    private void saveRecord(String batchId, String userId, String email, String phone,
                            NotificationChannel channel, BroadcastNotificationRequest req,
                            long scheduledAt, String recurrenceKey, String recurrenceDate, String recurrenceSlot) {
        NotificationParams params = NotificationParams.builder()
                .userId(userId).email(email).phone(phone)
                .title(req.getTitle()).message(req.getMessage())
                .type(req.getType()).channel(channel)
                .redirectUrl(req.getRedirectUrl()).referenceId(req.getReferenceId())
                .priority(req.getPriority()).batchId(batchId)
                .sendMode(req.getSendMode()).scheduledAt(scheduledAt)
                .maxRetries(req.getMaxRetries())
                .recurrenceKey(recurrenceKey).recurrenceDate(recurrenceDate).recurrenceSlot(recurrenceSlot)
                .build();
        if (!repository.saveIdempotent(buildNotification(params))) {
            log.info("Duplicate broadcast notification skipped for user {} channel {}", userId, channel);
        }
    }

    private Notification buildNotification(NotificationParams p) {
        Notification n = new Notification();
        n.setUserId(p.userId);
        n.setRecipientEmail(p.email);
        n.setRecipientPhone(p.phone);
        n.setTitle(p.title);
        n.setMessage(p.message);
        n.setType(p.type);
        n.setChannel(p.channel);
        n.setRedirectUrl(p.redirectUrl);
        n.setReferenceId(p.referenceId);
        n.setBatchId(p.batchId);

        String idempotencyKey = p.type.name() + "_" + p.userId
                + "_" + (p.referenceId != null ? p.referenceId : "NONE")
                + "_" + p.channel.name();
        if (p.recurrenceKey != null) {
            idempotencyKey += "_" + p.recurrenceKey;
        } else if (p.batchId != null) {
            idempotencyKey += "_" + p.batchId;
        }
        n.setIdempotencyKey(idempotencyKey);
        n.setNotificationId(UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8)).toString());

        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(p.sendMode);
        n.setPriority(p.priority != null ? p.priority : org.notification.model.enums.NotificationPriority.NORMAL);
        n.setScheduledAt(p.scheduledAt);

        long now = System.currentTimeMillis();
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        n.setExpirationTime((now / 1000) + (30L * 24 * 60 * 60));

        n.setMaxRetries(p.maxRetries != null ? p.maxRetries : defaultMaxRetries);
        n.setRetryCount(0);

        if (p.recurrenceKey != null) {
            n.setRecurrenceKey(p.recurrenceKey);
            n.setRecurrenceDate(p.recurrenceDate);
            n.setRecurrenceSlot(p.recurrenceSlot);
        }

        return n;
    }

    private static final class NotificationParams {
        final String userId;
        final String email;
        final String phone;
        final String title;
        final String message;
        final String redirectUrl;
        final String referenceId;
        final String batchId;
        final String recurrenceKey;
        final String recurrenceDate;
        final String recurrenceSlot;
        final org.notification.model.enums.NotificationType type;
        final NotificationChannel channel;
        final org.notification.model.enums.NotificationPriority priority;
        final org.notification.model.enums.SendMode sendMode;
        final long scheduledAt;
        final Integer maxRetries;

        private NotificationParams(Builder b) {
            this.userId = b.userId;
            this.email = b.email;
            this.phone = b.phone;
            this.title = b.title;
            this.message = b.message;
            this.redirectUrl = b.redirectUrl;
            this.referenceId = b.referenceId;
            this.batchId = b.batchId;
            this.recurrenceKey = b.recurrenceKey;
            this.recurrenceDate = b.recurrenceDate;
            this.recurrenceSlot = b.recurrenceSlot;
            this.type = b.type;
            this.channel = b.channel;
            this.priority = b.priority;
            this.sendMode = b.sendMode;
            this.scheduledAt = b.scheduledAt;
            this.maxRetries = b.maxRetries;
        }

        static Builder builder() { return new Builder(); }

        static final class Builder {
            String userId;
            String email;
            String phone;
            String title;
            String message;
            String redirectUrl;
            String referenceId;
            String batchId;
            String recurrenceKey;
            String recurrenceDate;
            String recurrenceSlot;
            org.notification.model.enums.NotificationType type;
            NotificationChannel channel;
            org.notification.model.enums.NotificationPriority priority;
            org.notification.model.enums.SendMode sendMode;
            long scheduledAt;
            Integer maxRetries;

            Builder userId(String v) { this.userId = v; return this; }
            Builder email(String v) { this.email = v; return this; }
            Builder phone(String v) { this.phone = v; return this; }
            Builder title(String v) { this.title = v; return this; }
            Builder message(String v) { this.message = v; return this; }
            Builder redirectUrl(String v) { this.redirectUrl = v; return this; }
            Builder referenceId(String v) { this.referenceId = v; return this; }
            Builder batchId(String v) { this.batchId = v; return this; }
            Builder recurrenceKey(String v) { this.recurrenceKey = v; return this; }
            Builder recurrenceDate(String v) { this.recurrenceDate = v; return this; }
            Builder recurrenceSlot(String v) { this.recurrenceSlot = v; return this; }
            Builder type(org.notification.model.enums.NotificationType v) { this.type = v; return this; }
            Builder channel(NotificationChannel v) { this.channel = v; return this; }
            Builder priority(org.notification.model.enums.NotificationPriority v) { this.priority = v; return this; }
            Builder sendMode(org.notification.model.enums.SendMode v) { this.sendMode = v; return this; }
            Builder scheduledAt(long v) { this.scheduledAt = v; return this; }
            Builder maxRetries(Integer v) { this.maxRetries = v; return this; }
            NotificationParams build() { return new NotificationParams(this); }
        }
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
        NotificationParams params = NotificationParams.builder()
                .userId(req.getUserId()).email(req.getEmail()).phone(req.getPhoneNumber())
                .title(req.getTitle()).message(req.getMessage())
                .type(req.getType()).channel(req.getChannel())
                .redirectUrl(req.getRedirectUrl()).referenceId(req.getReferenceId())
                .priority(req.getPriority()).sendMode(SendMode.SEND_NOW)
                .scheduledAt(System.currentTimeMillis()).maxRetries(defaultMaxRetries)
                .build();
        if (!repository.saveIdempotent(buildNotification(params))) {
            log.info("Duplicate immediate notification skipped for user {} channel {}", req.getUserId(), req.getChannel());
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#req.userId"),
            @CacheEvict(value = "unreadCount", key = "#req.userId")
    })
    public void sendInternal(NotificationRequest req) {
        if (req.getChannels() == null || req.getChannels().isEmpty()) {
            if (req.getChannel() != null) {
                req.setChannels(java.util.Collections.singletonList(req.getChannel().name()));
            } else {
                req.setChannels(java.util.Collections.singletonList(NotificationChannel.IN_APP.name()));
            }
        }

        boolean fetchContact = false;
        for (String c : req.getChannels()) {
            if ("EMAIL".equals(c) && (req.getEmail() == null || req.getEmail().isBlank())) {
                fetchContact = true;
            }
            if ("SMS".equals(c) && (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank())) {
                fetchContact = true;
            }
        }

        if (fetchContact) {
            try {
                UserNotificationTargetDTO contact = userClient.getUserContact(req.getUserId());
                if (contact != null) {
                    if (req.getEmail() == null || req.getEmail().isBlank()) {
                        req.setEmail(contact.getEmail());
                    }
                    if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
                        req.setPhoneNumber(contact.getPhoneNumber());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch contact details for user {}: {}", req.getUserId(), e.getMessage());
            }
        }

        for (String cStr : req.getChannels()) {
            try {
                NotificationChannel channel = NotificationChannel.valueOf(cStr);

                if (channel == NotificationChannel.EMAIL && (req.getEmail() == null || req.getEmail().isBlank())) {
                    log.warn("Skipping EMAIL for user {} due to missing email address.", req.getUserId());
                    continue;
                }
                if (channel == NotificationChannel.SMS && (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank())) {
                    log.warn("Skipping SMS for user {} due to missing phone number.", req.getUserId());
                    continue;
                }

                NotificationParams params = NotificationParams.builder()
                        .userId(req.getUserId()).email(req.getEmail()).phone(req.getPhoneNumber())
                        .title(req.getTitle()).message(req.getMessage())
                        .type(req.getType()).channel(channel)
                        .redirectUrl(req.getRedirectUrl()).referenceId(req.getReferenceId())
                        .priority(req.getPriority()).sendMode(SendMode.SEND_NOW)
                        .scheduledAt(System.currentTimeMillis()).maxRetries(defaultMaxRetries)
                        .build();
                if (repository.saveIdempotent(buildNotification(params))) {
                    log.info("{} notification queued/created for user {}", channel, req.getUserId());
                } else {
                    log.info("Duplicate internal notification skipped for user {} channel {}", req.getUserId(), channel);
                }
            } catch (Exception e) {
                log.error("Failed to process channel {} for user {}", cStr, req.getUserId(), e);
            }
        }
    }

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