package org.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.notification.model.Notification;
import org.notification.model.InAppNotification;
import org.notification.repository.InAppRepository;
import org.notification.exception.InvalidUserIdException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class InAppChannel implements NotificationChannel {

    private static final String CHANNEL_NAME = "IN_APP";

    private final InAppRepository repo;

    public InAppChannel(InAppRepository repo) {
        this.repo = repo;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(Notification n) {

        if (n == null || n.getUserId() == null
                || n.getUserId().isBlank()) {

            log.error("UserId is missing");

            throw new InvalidUserIdException(
                    "UserId is required for IN_APP channel"
            );
        }

        log.info("Received IN_APP notification for user {}", n.getUserId());

        InAppNotification notif = new InAppNotification();
        notif.setId(UUID.randomUUID().toString());
        notif.setUserId(n.getUserId());
        notif.setTitle(n.getTitle());
        notif.setDescription(n.getDescription());
        notif.setType(n.getType());
        notif.setCreatedAt(System.currentTimeMillis());
        notif.setIsRead(false);
        notif.setRedirectUrl(n.getRedirectUrl());

        log.info("Saving notification to DynamoDB");

        repo.save(notif);

        log.info("Notification saved successfully with id {}", notif.getId());
    }
}
