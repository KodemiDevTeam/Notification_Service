package org.Notification.service;

import org.Notification.model.InAppNotification;
import org.Notification.model.enums.NotificationType;
import org.Notification.repository.InAppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InAppService {

    @Autowired
    private InAppRepository inAppRepo;

    // ✅ Correct single method
    public void createInApp(String userId,
                            String title,
                            String desc,
                            NotificationType type,
                            String redirectUrl) {

        try {

            System.out.println("Creating IN_APP notification");

            InAppNotification inApp =
                    new InAppNotification();

            inApp.setId(
                    UUID.randomUUID().toString());

            inApp.setUserId(userId);

            inApp.setTitle(title);

            inApp.setDescription(desc);

            inApp.setType(type);

            inApp.setCreatedAt(
                    System.currentTimeMillis());

            inApp.setIsRead(false);

            inApp.setRedirectUrl(redirectUrl);

            // ✅ Save to DynamoDB
            inAppRepo.save(inApp);

            System.out.println(
                    "IN_APP saved successfully: "
                            + inApp.getId()
            );

        } catch (Exception e) {

            System.out.println(
                    "IN_APP FAILED: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}