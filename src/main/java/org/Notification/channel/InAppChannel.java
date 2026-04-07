package org.Notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.Notification.model.Notification;
import org.Notification.model.InAppNotification;
import org.Notification.repository.InAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class InAppChannel implements NotificationChannel {

    @Autowired
    private InAppRepository repo;

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    public void send(Notification n) {

        log.info("Received IN_APP notification for user {}", n.getUserId());

        if (n.getUserId() == null || n.getUserId().isEmpty()) {
            log.error("UserId is missing");
            throw new RuntimeException("UserId is required");
        }

        InAppNotification notif = new InAppNotification();

        notif.setId(UUID.randomUUID().toString());
        notif.setUserId(n.getUserId());
        notif.setMessage(n.getMessage());
        notif.setStatus("SENT");

        //  FIXED
        notif.setCreatedAt(System.currentTimeMillis());

        notif.setIsRead(false);

        log.info("Saving notification to DynamoDB");

        repo.save(notif);

        log.info("Notification saved successfully with id {}", notif.getId());
    }
}
