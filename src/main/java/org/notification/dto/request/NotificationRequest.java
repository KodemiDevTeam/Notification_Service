package org.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationType;
import org.notification.model.enums.NotificationPriority;

@Data
public class NotificationRequest {
    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "type is required")
    private NotificationType type;
    
    private NotificationChannel channel;
    
    private java.util.List<String> channels;

    private NotificationPriority priority;

    private String email;
    private String phoneNumber;
    private String redirectUrl;
    private String referenceId;
}
