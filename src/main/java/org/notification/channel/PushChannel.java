package org.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.notification.model.Notification;
import org.notification.exception.InvalidDeviceTokenException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushChannel implements NotificationChannel {

    private static final String CHANNEL_NAME = "PUSH";

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(Notification n) {

        if (n == null || n.getDeviceToken() == null
                || n.getDeviceToken().isBlank()) {

            log.error("Device token is missing");

            throw new InvalidDeviceTokenException(
                    "Device token is required for PUSH channel"
            );
        }

        log.info("Received PUSH notification");

        log.info("To Device: {}", n.getDeviceToken());
        log.info("Message: {}", n.getMessage());

        log.info("Push notification processed successfully");
    }
}