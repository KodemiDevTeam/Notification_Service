package org.notification.service;

import lombok.extern.slf4j.Slf4j;

import org.notification.model.InAppNotification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.InAppRepository;
import org.notification.exception.InAppNotificationException;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class InAppService {

    private static final boolean DEFAULT_READ_STATUS = false;

    private final InAppRepository inAppRepo;

    // Constructor Injection (Sonar preferred)
    public InAppService(InAppRepository inAppRepo) {
        this.inAppRepo = inAppRepo;
    }

    // CREATE IN-APP NOTIFICATION
    public void createInApp(
            String userId,
            String title,
            String desc,
            NotificationType type,
            String redirectUrl) {

        // ==============================
        // INPUT VALIDATION
        // ==============================

        if (userId == null || userId.isBlank()) {

            log.error("UserId is missing");

            throw new IllegalArgumentException(
                    "UserId is required"
            );
        }

        if (title == null || title.isBlank()) {

            log.error("Title is missing");

            throw new IllegalArgumentException(
                    "Title is required"
            );
        }

        if (desc == null || desc.isBlank()) {

            log.error("Description is missing");

            throw new IllegalArgumentException(
                    "Description is required"
            );
        }

        if (type == null) {

            log.error("Notification type is missing");

            throw new IllegalArgumentException(
                    "Notification type is required"
            );
        }

        try {

            log.info(
                    "Creating IN_APP notification for user {}",
                    userId
            );

            InAppNotification inApp =
                    new InAppNotification();

            // Generate ID
            String id =
                    UUID.randomUUID().toString();

            inApp.setId(id);

            inApp.setUserId(userId);

            inApp.setTitle(title);

            inApp.setDescription(desc);

            inApp.setType(type);

            inApp.setCreatedAt(
                    System.currentTimeMillis()
            );

            inApp.setIsRead(DEFAULT_READ_STATUS);

            inApp.setRedirectUrl(redirectUrl);

            // Save to DynamoDB
            inAppRepo.save(inApp);

            log.info(
                    "IN_APP saved successfully with id {}",
                    id
            );

        } catch (Exception e) {

            log.error(
                    "IN_APP creation failed for user {}",
                    userId,
                    e
            );

            throw new InAppNotificationException(
                    "Failed to create IN_APP notification",
                    e
            );
        }
    }
}