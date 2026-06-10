package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InAppNotificationService {

    public void sendInApp(String userId, String title) {
        log.info("IN_APP notification registered for user {}: {}", userId, title);
    }
}
