package org.Notification.scheduler;

import org.Notification.model.Notification;
import org.Notification.repository.NotificationRepository;
import org.Notification.service.ChannelDispatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationScheduler {

    @Autowired
    private NotificationRepository repo;

    @Autowired
    private ChannelDispatcherService dispatcher;

    @Value("${notification.scheduler.max-retry}")
    private int maxRetry;

    @Scheduled(fixedDelayString = "${notification.scheduler.interval}")
    public void run() {

        System.out.println("Scheduler running...");

        List<Notification> list = repo.findAll();

        if (list.isEmpty()) {
            System.out.println("No data found in DB");
            return;
        }

        boolean hasData = false;

        for (Notification n : list) {

            if (n == null) continue;

            hasData = true;

            // ✅ Only process PENDING notifications
            if ("PENDING".equalsIgnoreCase(n.getStatus())) {

                try {
                    // 🔥 Send notification
                    dispatcher.dispatch(n);

                    // ✅ Update status
                    n.setStatus("SENT");
                    repo.save(n);

                } catch (Exception e) {

                    int retry = n.getRetryCount() == null ? 0 : n.getRetryCount();

                    if (retry < maxRetry) {
                        n.setRetryCount(retry + 1);
                        n.setStatus("PENDING");
                    } else {
                        n.setStatus("FAILED");
                    }

                    repo.save(n);
                }
            }
        }

        if (!hasData) {
            System.out.println("No data from DB");
        }
    }
}