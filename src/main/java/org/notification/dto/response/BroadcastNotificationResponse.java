package org.notification.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BroadcastNotificationResponse {
    private String batchId;
    private int totalUsers;
    private int totalNotificationsCreated;
    private int inAppCreated;
    private int emailCreated;
    private int smsCreated;
    private int skippedEmailMissing;
    private int skippedPhoneMissing;
    private String message;
}
