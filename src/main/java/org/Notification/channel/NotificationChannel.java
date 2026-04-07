package org.Notification.channel;

import org.Notification.model.Notification;

public interface NotificationChannel {
    String getChannelName();
    void send(Notification notification);
}
