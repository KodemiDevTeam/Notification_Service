package org.notification.client;

import org.notification.dto.response.UserNotificationTargetDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/v1/learner/notification-targets")
    List<UserNotificationTargetDTO> getUsersForNotification(@RequestParam(value = "role", required = false) String role);

    @GetMapping("/api/v1/user/internal/contact/{userId}")
    UserNotificationTargetDTO getUserContact(@org.springframework.web.bind.annotation.PathVariable("userId") String userId);
}
