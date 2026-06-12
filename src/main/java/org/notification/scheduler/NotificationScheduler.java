package org.notification.scheduler;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import lombok.extern.slf4j.Slf4j;
import org.notification.exception.PermanentFailureException;
import org.notification.exception.ProviderDisabledException;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.SendMode;
import org.notification.repository.NotificationRepository;
import org.notification.service.NotificationDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

    private static final long TTL_7_DAYS_SECONDS = 7L * 24 * 60 * 60;
    private static final long RETRY_DELAY_MS = 300_000L; // 5 minutes

    private final NotificationRepository repository;
    private final NotificationDispatcher dispatcher;
    private final DynamoDBMapper dynamoDBMapper;

    @Value("${notification.scheduler.batch-size:50}")
    private int batchSize;

    @Value("${notification.recurring.timezone:Asia/Kolkata}")
    private String defaultTimezone;

    public NotificationScheduler(NotificationRepository repository,
                                 NotificationDispatcher dispatcher,
                                 DynamoDBMapper dynamoDBMapper) {
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:60000}")
    public void processPendingNotifications() {
        long now = System.currentTimeMillis();
        log.info("Scheduler running at {}...", now);

        List<Notification> dueList = collectAndSort(now);
        if (dueList.isEmpty()) return;

        dueList.forEach(this::processIfLockAcquired);
    }

    private void processIfLockAcquired(Notification notification) {
        if (tryAcquireLock(notification)) {
            processNotification(notification);
        }
    }

    private List<Notification> collectAndSort(long now) {
        List<Notification> dueList = new ArrayList<>();
        dueList.addAll(repository.findDueNotifications(now, batchSize, NotificationStatus.PENDING));
        dueList.addAll(repository.findDueNotifications(now, batchSize, NotificationStatus.RETRY_SCHEDULED));
        dueList.sort((a, b) -> {
            long s1 = a.getScheduledAt() != null ? a.getScheduledAt() : 0L;
            long s2 = b.getScheduledAt() != null ? b.getScheduledAt() : 0L;
            return Long.compare(s1, s2);
        });
        return dueList.size() > batchSize ? dueList.subList(0, batchSize) : dueList;
    }

    private boolean tryAcquireLock(Notification notification) {
        try {
            boolean locked = repository.acquireLock(
                    notification.getNotificationId(), notification.getStatus(), NotificationStatus.PROCESSING);
            if (locked) {
                notification.setStatus(NotificationStatus.PROCESSING);
            } else {
                log.warn("Failed to lock notification {}: already processed.", notification.getNotificationId());
            }
            return locked;
        } catch (Exception e) {
            log.warn("Exception while locking notification {}: {}", notification.getNotificationId(), e.getMessage());
            return false;
        }
    }

    private void processNotification(Notification notification) {
        try {
            dispatcher.dispatch(notification);
            markSent(notification);
            scheduleNextRecurrenceIfApplicable(notification);
        } catch (ProviderDisabledException e) {
            markSkipped(notification, e.getMessage());
            scheduleNextRecurrenceIfApplicable(notification);
        } catch (PermanentFailureException e) {
            log.error("Permanent failure sending notification {}", notification.getNotificationId(), e);
            markFailed(notification, e.getMessage());
            scheduleNextRecurrenceIfApplicable(notification);
        } catch (Exception e) {
            log.error("Temporary failure sending notification {}", notification.getNotificationId(), e);
            handleTemporaryFailure(notification, e.getMessage());
        }
    }

    private void markSent(Notification n) {
        long now = System.currentTimeMillis();
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(now);
        n.setUpdatedAt(now);
        n.setExpirationTime((now / 1000) + TTL_7_DAYS_SECONDS);
        n.setFailureReason(null);
        repository.save(n);
    }

    private void markSkipped(Notification n, String reason) {
        long now = System.currentTimeMillis();
        log.warn("Provider disabled for notification {}, marking SKIPPED", n.getNotificationId());
        n.setStatus(NotificationStatus.SKIPPED);
        n.setFailureReason(reason);
        n.setUpdatedAt(now);
        n.setExpirationTime((now / 1000) + TTL_7_DAYS_SECONDS);
        repository.save(n);
    }

    private void markFailed(Notification n, String reason) {
        long now = System.currentTimeMillis();
        n.setStatus(NotificationStatus.FAILED);
        n.setFailedAt(now);
        n.setFailureReason(reason);
        n.setUpdatedAt(now);
        n.setExpirationTime((now / 1000) + TTL_7_DAYS_SECONDS);
        repository.save(n);
    }

    private void handleTemporaryFailure(Notification n, String reason) {
        int retries = (n.getRetryCount() != null ? n.getRetryCount() : 0) + 1;
        int maxRetries = n.getMaxRetries() != null ? n.getMaxRetries() : 3;

        n.setRetryCount(retries);
        n.setFailureReason(reason);
        n.setUpdatedAt(System.currentTimeMillis());

        if (retries >= maxRetries) {
            markFailed(n, reason);
            scheduleNextRecurrenceIfApplicable(n);
        } else {
            long nextRetry = System.currentTimeMillis() + RETRY_DELAY_MS;
            n.setStatus(NotificationStatus.RETRY_SCHEDULED);
            n.setNextRetryAt(nextRetry);
            n.setScheduledAt(nextRetry);
            repository.save(n);
        }
    }

    private void scheduleNextRecurrenceIfApplicable(Notification current) {
        if (!isRecurring(current.getSendMode())) return;

        ZoneId zoneId = ZoneId.of(defaultTimezone);
        LocalDate date = Instant.ofEpochMilli(current.getScheduledAt()).atZone(zoneId).toLocalDate().plusDays(1);
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String slot = current.getRecurrenceSlot() != null ? current.getRecurrenceSlot() : "DAILY";

        String newRecurrenceKey = String.format("%s_%s_%s_%s_%s",
                current.getBatchId(), current.getUserId(),
                current.getChannel().name(), dateStr, slot);

        if (!repository.existsByRecurrenceKey(newRecurrenceKey)) {
            dynamoDBMapper.save(buildNextRecurrence(current, newRecurrenceKey, dateStr, slot));
            log.info("Scheduled next recurring notification for batch {} user {} date {}",
                    current.getBatchId(), current.getUserId(), dateStr);
        }
    }

    private static boolean isRecurring(SendMode mode) {
        return mode == SendMode.DAILY_MORNING
                || mode == SendMode.DAILY_EVENING
                || mode == SendMode.DAILY_MORNING_EVENING
                || mode == SendMode.CUSTOM_RECURRING;
    }

    private Notification buildNextRecurrence(Notification current, String recurrenceKey, String dateStr, String slot) {
        Notification next = new Notification();
        next.setNotificationId(UUID.randomUUID().toString());
        next.setBatchId(current.getBatchId());
        next.setUserId(current.getUserId());
        next.setRecipientEmail(current.getRecipientEmail());
        next.setRecipientPhone(current.getRecipientPhone());
        next.setTitle(current.getTitle());
        next.setMessage(current.getMessage());
        next.setType(current.getType());
        next.setChannel(current.getChannel());
        next.setStatus(NotificationStatus.PENDING);
        next.setSendMode(current.getSendMode());
        next.setScheduledAt(current.getScheduledAt() + 86_400_000L);
        long now = System.currentTimeMillis();
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        next.setMaxRetries(current.getMaxRetries());
        next.setRedirectUrl(current.getRedirectUrl());
        next.setRecurrenceKey(recurrenceKey);
        next.setRecurrenceDate(dateStr);
        next.setRecurrenceSlot(slot);
        return next;
    }
}
