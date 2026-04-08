package org.notification.scheduler;

import lombok.extern.slf4j.Slf4j;

import org.notification.model.Notification;
import org.notification.repository.NotificationRepository;
import org.notification.service.ChannelDispatcherService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class NotificationScheduler {

    // ==============================
    // CONSTANTS (Removes Magic Strings)
    // ==============================

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final NotificationRepository repo;
    private final ChannelDispatcherService dispatcher;

    private final int maxRetry;

    // Constructor Injection
    public NotificationScheduler(
            NotificationRepository repo,
            ChannelDispatcherService dispatcher,
            @Value("${notification.scheduler.max-retry}")
            int maxRetry) {

        this.repo = repo;
        this.dispatcher = dispatcher;
        this.maxRetry = maxRetry;
    }

    // ==============================
    // MAIN SCHEDULER
    // ==============================

    @Scheduled(
            fixedDelayString =
                    "${notification.scheduler.interval}"
    )
    public void run() {

        log.info("Scheduler running...");

        List<Notification> list = repo.findAll();

        if (list.isEmpty()) {
            log.info("No data found in DB");
            return;
        }

        processNotifications(list);
    }

    // ==============================
    // PROCESS LIST
    // ==============================

    private void processNotifications(
            List<Notification> list) {

        for (Notification n : list) {

            if (n == null) {
                continue;
            }

            if (isPending(n)) {
                handleNotification(n);
            }
        }
    }

    // ==============================
    // CHECK STATUS
    // ==============================

    private boolean isPending(Notification n) {

        return STATUS_PENDING
                .equalsIgnoreCase(n.getStatus());
    }

    // ==============================
    // HANDLE NOTIFICATION
    // ==============================

    private void handleNotification(Notification n) {

        try {

            log.info("Dispatching notification id={}",
                    n.getNotificationId());

            dispatcher.dispatch(n);

            markAsSent(n);

        } catch (Exception e) {

            log.error("Error dispatching notification id={}",
                    n.getNotificationId(), e);

            handleRetry(n);
        }

        repo.save(n);
    }

    // ==============================
    // SUCCESS LOGIC
    // ==============================

    private void markAsSent(Notification n) {

        n.setStatus(STATUS_SENT);

        log.info("Notification sent successfully id={}",
                n.getNotificationId());
    }

    // ==============================
    // RETRY LOGIC
    // ==============================

    private void handleRetry(Notification n) {

        int retry =
                n.getRetryCount() == null
                        ? 0
                        : n.getRetryCount();

        if (retry < maxRetry) {

            n.setRetryCount(retry + 1);
            n.setStatus(STATUS_PENDING);

            log.warn(
                    "Retrying notification id={}, retry={}",
                    n.getNotificationId(),
                    retry + 1
            );

        } else {

            n.setStatus(STATUS_FAILED);

            log.error(
                    "Notification failed permanently id={}",
                    n.getNotificationId()
            );
        }
    }

}