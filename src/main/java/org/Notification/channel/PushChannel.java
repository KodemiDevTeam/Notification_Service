package org.Notification.channel;

import org.Notification.model.Notification;
import org.springframework.stereotype.Service;

@Service
public class PushChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "PUSH";
    }

    @Override
    public void send(Notification n) {

        if (n.getDeviceToken() == null || n.getDeviceToken().isEmpty()) {
            throw new RuntimeException("Device token is required for PUSH channel");
        }

        System.out.println("===== PUSH NOTIFICATION =====");
        System.out.println("To Device: " + n.getDeviceToken());
        System.out.println("Message: " + n.getMessage());
        System.out.println("=============================");
    }
}
