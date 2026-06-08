package org.notification.client;

import org.notification.dto.response.UserNotificationTargetDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public List<UserNotificationTargetDTO> getUsersForNotification(String role) {
                log.error("Failed to retrieve user notification targets. Role: {}. Fallback fail-silent returning empty list. Error: {}", role, cause.getMessage(), cause);
                return Collections.emptyList();
            }

            @Override
            public UserNotificationTargetDTO getUserContact(String userId) {
                log.error("Failed to retrieve user contact details. UserId: {}. Fallback fail-silent returning null. Error: {}", userId, cause.getMessage(), cause);
                return null;
            }
        };
    }
}
