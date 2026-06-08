package org.notification.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InAppNotificationServiceTest {

    @Test
    void testSendInApp() {
        InAppNotificationService service = new InAppNotificationService();
        assertDoesNotThrow(() -> service.sendInApp("u1", "Title", "Body"));
    }
}
