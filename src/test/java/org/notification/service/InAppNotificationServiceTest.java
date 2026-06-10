package org.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InAppNotificationServiceTest {

    private final InAppNotificationService service = new InAppNotificationService();

    @Test
    void testSendInApp_LogsAndDoesNotThrow() {
        assertDoesNotThrow(() -> service.sendInApp("u1", "Title"));
    }

    @Test
    void testSendInApp_NullValues_DoesNotThrow() {
        assertDoesNotThrow(() -> service.sendInApp(null, null));
    }
}
