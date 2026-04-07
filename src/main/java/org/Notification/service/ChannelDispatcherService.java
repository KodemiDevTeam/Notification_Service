package org.Notification.service;

import org.Notification.channel.NotificationChannel;
import org.Notification.model.Notification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChannelDispatcherService {

    private final Map<String, NotificationChannel> channelMap = new HashMap<>();
    private final int maxRetry = 3;

    public ChannelDispatcherService(List<NotificationChannel> channels) {
        for (NotificationChannel c : channels) {
            channelMap.put(c.getChannelName(), c);
        }
    }

    public void dispatch(Notification n) {

        if (n == null || n.getChannel() == null) {
            System.out.println("Invalid notification");
            return;
        }

        NotificationChannel channel = channelMap.get(n.getChannel());

        if (channel == null) {
            System.out.println("No channel found for " + n.getChannel());
            return;
        }

        try {
            channel.send(n);
            System.out.println("Notification sent via " + n.getChannel());
        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());

            int retry = n.getRetryCount() == null ? 0 : n.getRetryCount();

            if (retry >= maxRetry) {
                System.out.println("Max retries reached for " + n.getNotificationId());
                n.setStatus("FAILED");
            } else {
                n.setRetryCount(retry + 1);
                dispatch(n); // retry
            }
        }
    }
}