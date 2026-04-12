package org.notification.service;

import lombok.extern.slf4j.Slf4j;

import org.notification.channel.NotificationChannel;
import org.notification.model.Notification;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChannelDispatcherService {

    // ==============================
    // CONSTANTS (Sonar-safe)
    // ==============================

    private static final int MAX_RETRY = 3;

    private static final String STATUS_FAILED = "FAILED";

    private final Map<String, NotificationChannel> channelMap =
            new HashMap<>();

    // Constructor Injection
    public ChannelDispatcherService(
            List<NotificationChannel> channels) {

        for (NotificationChannel c : channels) {

            channelMap.put(
                    c.getChannelName(),
                    c
            );
        }
    }

    // ==============================
    // MAIN DISPATCH METHOD
    // ==============================

    public void dispatch(Notification n) {

        if (n == null || n.getChannel() == null) {

            log.error("Invalid notification or channel is null");

            return;
        }

        NotificationChannel channel =
                channelMap.get(n.getChannel());

        if (channel == null) {

            log.error(
                    "No channel found for {}",
                    n.getChannel()
            );

            return;
        }

        sendNotification(n, channel);
    }

    // ==============================
    // SEND LOGIC
    // ==============================

    private void sendNotification(
            Notification n,
            NotificationChannel channel) {

        try {

            channel.send(n);

            log.info(
                    "Notification sent via {}",
                    n.getChannel()
            );

        } catch (Exception e) {

            log.error(
                    "Error sending notification id={}",
                    n.getNotificationId(),
                    e
            );

            handleRetry(n);
        }
    }

    // ==============================
    // RETRY LOGIC
    // ==============================

    private void handleRetry(Notification n) {

        int retry =
                n.getRetryCount() == null
                        ? 0
                        : n.getRetryCount();

        if (retry >= MAX_RETRY) {

            log.error(
                    "Max retries reached for {}",
                    n.getNotificationId()
            );

            n.setStatus(STATUS_FAILED);

        } else {

            n.setRetryCount(retry + 1);

            log.warn(
                    "Retrying notification id={}, retry={}",
                    n.getNotificationId(),
                    retry + 1
            );
        }
    }

}