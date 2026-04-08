package org.notification.channel;

import org.notification.model.Notification;

public interface NotificationChannel {
    String getChannelName();
    void send(Notification notification);
}
