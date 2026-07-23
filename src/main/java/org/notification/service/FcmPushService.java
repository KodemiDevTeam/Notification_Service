package org.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.notification.model.DeviceToken;
import org.notification.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;

    public FcmPushService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public void sendPushNotification(String userId, String title, String body, String type, String referenceId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase is not initialized. Skipping push notification for userId: {}", userId);
            return;
        }

        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.info("No registered mobile device tokens found for userId: {}", userId);
            return;
        }

        log.info("Sending push notification to {} registered devices for userId: {}", deviceTokens.size(), userId);
        
        for (DeviceToken deviceToken : deviceTokens) {
            try {
                // Build notification structure for system tray display
                Notification notification = Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build();

                // Build message with data payload for foreground/navigation routing
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(notification)
                        .putData("title", title)
                        .putData("message", body)
                        .putData("type", type != null ? type : "GENERAL")
                        .putData("referenceId", referenceId != null ? referenceId : "")
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent push notification to token {}. Response: {}", deviceToken.getToken(), response);
                
            } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
                log.warn("Firebase Messaging Exception for token {}: {}. ErrorCode: {}", 
                        deviceToken.getToken(), e.getMessage(), e.getMessagingErrorCode());
                
                // If token is invalid or inactive, deregister it to keep DB clean
                if (e.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("Deregistering invalid FCM token: {}", deviceToken.getToken());
                    deviceTokenRepository.delete(deviceToken);
                }
            } catch (Exception e) {
                log.error("Failed to send push notification to token {}: {}", deviceToken.getToken(), e.getMessage(), e);
            }
        }
    }
}
