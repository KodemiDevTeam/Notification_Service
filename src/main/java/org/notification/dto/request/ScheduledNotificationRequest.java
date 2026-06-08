package org.notification.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationType;

@Data
public class ScheduledNotificationRequest {
    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotNull(message = "channel is required")
    private NotificationChannel channel;

    private String email;
    private String phoneNumber;
    private String redirectUrl;

    @NotNull(message = "scheduledTime is required")
    @Future(message = "scheduledTime must be in the future")
    private Long scheduledTime;
}
