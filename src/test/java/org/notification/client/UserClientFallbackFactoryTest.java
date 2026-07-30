package org.notification.client;

import org.junit.jupiter.api.Test;
import org.notification.dto.response.UserNotificationTargetDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserClientFallbackFactoryTest {

    @Test
    void testUserClientFallbackFactory() {
        UserClientFallbackFactory factory = new UserClientFallbackFactory();
        Throwable cause = new RuntimeException("Service down");

        UserClient fallbackClient = factory.create(cause);
        assertNotNull(fallbackClient);

        List<UserNotificationTargetDTO> targets = fallbackClient.getUsersForNotification("LEARNER");
        assertNotNull(targets);
        assertTrue(targets.isEmpty());

        UserNotificationTargetDTO contact = fallbackClient.getUserContact("u1", "secret");
        assertNull(contact);
    }
}
