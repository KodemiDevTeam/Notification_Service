package model;

import org.Notification.model.Notification;
import org.Notification.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Notification n = new Notification();
        n.setNotificationId("id-1");
        n.setUserId("user1");
        n.setTitle("Test Title");
        n.setDescription("Test Desc");
        n.setChannel("EMAIL");
        n.setType(NotificationType.COURSE_ALERT);
        n.setStatus("PENDING");
        n.setRetryCount(0);

        assertEquals("id-1", n.getNotificationId());
        assertEquals("user1", n.getUserId());
        assertEquals("Test Title", n.getTitle());
        assertEquals("Test Desc", n.getDescription());
        assertEquals("EMAIL", n.getChannel());
        assertEquals(NotificationType.COURSE_ALERT, n.getType());
        assertEquals("PENDING", n.getStatus());
        assertEquals(0, n.getRetryCount());
    }

    @Test
    void defaultValues_shouldBeNull() {
        Notification n = new Notification();
        assertNull(n.getNotificationId());
        assertNull(n.getUserId());
        assertNull(n.getStatus());
    }

    @Test
    void getMessage_shouldReturnEmptyString() {
        Notification n = new Notification();
        assertEquals("", n.getMessage());
    }
}
