package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InAppNotificationService {

    // IN_APP notifications are simply stored in DynamoDB for retrieval via GET /api/v1/notifications/user/{userId}
    // If you add WebSockets or Firebase FCM in the future, you can integrate it here.

    public void sendInApp(String userId, String title, String body) {
        log.info("IN_APP notification registered for user {}: {}", userId, title);
        // Firebase Cloud Messaging (FCM) logic can be injected here.
    }
}
