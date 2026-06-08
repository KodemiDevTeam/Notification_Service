package org.notification.dto.response;

import lombok.Builder;
import lombok.Data;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.NotificationType;

@Data
@Builder
public class NotificationResponse {
    private String notificationId;
    private String userId;
    private String title;
    private String description;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    private Boolean isRead;
    private Boolean isScheduled;
    private Long scheduledTime;
    private Long createdAt;
    private String redirectUrl;

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .notificationId(entity.getNotificationId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getMessage())
                .type(entity.getType())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .isRead(entity.getRead())
                .isScheduled(entity.getSendMode() != null && entity.getSendMode().name().contains("SCHEDULED"))
                .scheduledTime(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt())
                .redirectUrl(entity.getRedirectUrl())
                .build();
    }
}
