package org.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationType;
import org.notification.model.enums.NotificationPriority;
import org.notification.model.enums.SendMode;

import java.util.List;

@Data
public class BroadcastNotificationRequest {
    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotNull(message = "channels list is required")
    private List<NotificationChannel> channels;

    private String targetRole;

    @NotNull(message = "sendMode is required")
    private SendMode sendMode;
    
    private NotificationPriority priority;

    private String scheduledAt;
    
    private String morningTime;
    private String eveningTime;
    private String timezone;
    
    private Integer maxRetries;
    private String redirectUrl;
    private String referenceId;
}
