package org.notification.scheduler;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import lombok.extern.slf4j.Slf4j;
import org.notification.exception.PermanentFailureException;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

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

        List<Notification> pending = repository.findDueNotifications(now, batchSize, NotificationStatus.PENDING);
        List<Notification> retryScheduled = repository.findDueNotifications(now, batchSize, NotificationStatus.RETRY_SCHEDULED);

        List<Notification> dueList = new ArrayList<>();
        dueList.addAll(pending);
        dueList.addAll(retryScheduled);
        
        // Sort by scheduledAt
        dueList.sort(new Comparator<Notification>() {
            @Override
            public int compare(Notification n1, Notification n2) {
                Long s1 = n1.getScheduledAt() != null ? n1.getScheduledAt() : 0L;
                Long s2 = n2.getScheduledAt() != null ? n2.getScheduledAt() : 0L;
                return s1.compareTo(s2);
            }
        });

        // Enforce batch limit across both lists combined
        if (dueList.size() > batchSize) {
            dueList = dueList.subList(0, batchSize);
        }

        if (dueList.isEmpty()) {
            return;
        }

        for (Notification notification : dueList) {
            try {
                // Production-ready atomic lock using explicit DynamoDB UpdateItem
                boolean locked = repository.acquireLock(notification.getNotificationId(), notification.getStatus(), NotificationStatus.PROCESSING);
                if (!locked) {
                    log.warn("Failed to lock notification {}: picked up by another instance or already processed.", notification.getNotificationId());
                    continue;
                }
                // Update local object to reflect the new state
                notification.setStatus(NotificationStatus.PROCESSING);
            } catch (Exception e) {
                log.warn("Exception while locking notification {}: {}", notification.getNotificationId(), e.getMessage());
                continue;
            }

            try {
                // Dispatch
                dispatcher.dispatch(notification);
                
                // Success
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(System.currentTimeMillis());
                notification.setUpdatedAt(System.currentTimeMillis());
                notification.setExpirationTime((System.currentTimeMillis() / 1000) + (7L * 24 * 60 * 60));
                notification.setFailureReason(null);
                repository.save(notification); // Safe to use save() here since we hold the PROCESSING lock
                
                scheduleNextRecurrenceIfApplicable(notification);

            } catch (org.notification.exception.ProviderDisabledException e) {
                log.warn("Provider disabled for notification {}, marking SKIPPED", notification.getNotificationId());
                notification.setStatus(NotificationStatus.SKIPPED);
                notification.setFailureReason(e.getMessage());
                notification.setUpdatedAt(System.currentTimeMillis());
                notification.setExpirationTime((System.currentTimeMillis() / 1000) + (7L * 24 * 60 * 60));
                repository.save(notification);

                scheduleNextRecurrenceIfApplicable(notification);

            } catch (PermanentFailureException e) {
                log.error("Permanent failure sending notification {}", notification.getNotificationId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailedAt(System.currentTimeMillis());
                notification.setFailureReason(e.getMessage());
                notification.setUpdatedAt(System.currentTimeMillis());
                notification.setExpirationTime((System.currentTimeMillis() / 1000) + (7L * 24 * 60 * 60));
                repository.save(notification);

                scheduleNextRecurrenceIfApplicable(notification);

            } catch (Throwable e) {
                log.error("Temporary failure sending notification {}", notification.getNotificationId(), e);
                int retries = notification.getRetryCount() != null ? notification.getRetryCount() : 0;
                int maxRetries = notification.getMaxRetries() != null ? notification.getMaxRetries() : 3;
                
                retries++;
                notification.setRetryCount(retries);
                notification.setFailureReason(e.getMessage());
                notification.setUpdatedAt(System.currentTimeMillis());

                if (retries >= maxRetries) {
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setFailedAt(System.currentTimeMillis());
                    notification.setExpirationTime((System.currentTimeMillis() / 1000) + (7L * 24 * 60 * 60));
                    scheduleNextRecurrenceIfApplicable(notification);
                } else {
                    notification.setStatus(NotificationStatus.RETRY_SCHEDULED);
                    // Exponential backoff or fixed 5 minutes
                    long nextRetry = System.currentTimeMillis() + (300000L); // 5 minutes
                    notification.setNextRetryAt(nextRetry);
                    notification.setScheduledAt(nextRetry); // Update index key so it gets picked up
                }
                repository.save(notification);
            }
        }
    }

    private void scheduleNextRecurrenceIfApplicable(Notification current) {
        if (current.getSendMode() == SendMode.DAILY_MORNING ||
            current.getSendMode() == SendMode.DAILY_EVENING ||
            current.getSendMode() == SendMode.DAILY_MORNING_EVENING ||
            current.getSendMode() == SendMode.CUSTOM_RECURRING) {
            
            // Advance date by 1 day
            ZoneId zoneId = ZoneId.of(defaultTimezone);
            LocalDate date = Instant.ofEpochMilli(current.getScheduledAt()).atZone(zoneId).toLocalDate().plusDays(1);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String slot = current.getRecurrenceSlot();
            if (slot == null) slot = "DAILY"; // fallback

            String newRecurrenceKey = String.format("%s_%s_%s_%s_%s", 
                current.getBatchId(), 
                current.getUserId(), 
                current.getChannel().name(), 
                dateStr, 
                slot);

            if (!repository.existsByRecurrenceKey(newRecurrenceKey)) {
                Notification next = new Notification();
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
                
                // Add exactly 24 hours to the scheduled time
                next.setScheduledAt(current.getScheduledAt() + 86400000L); 
                
                next.setCreatedAt(System.currentTimeMillis());
                next.setUpdatedAt(System.currentTimeMillis());
                next.setMaxRetries(current.getMaxRetries());
                next.setRedirectUrl(current.getRedirectUrl());
                
                next.setRecurrenceKey(newRecurrenceKey);
                next.setRecurrenceDate(dateStr);
                next.setRecurrenceSlot(slot);

                dynamoDBMapper.save(next);
                log.info("Scheduled next recurring notification for batch {} user {} date {}", next.getBatchId(), next.getUserId(), dateStr);
            }
        }
    }
}