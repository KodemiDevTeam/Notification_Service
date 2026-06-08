package org.notification.dto.response;

import lombok.Data;

@Data
public class UserNotificationTargetDTO {
    private String userId;
    private String email;
    private String phoneNumber;
}
