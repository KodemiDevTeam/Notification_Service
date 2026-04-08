package model;

import org.notification.model.InAppNotification;
import org.notification.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InAppNotificationTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        InAppNotification n = new InAppNotification();
        n.setId("id-1");
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Desc");
        n.setType(NotificationType.COURSE_ALERT);
        n.setIsRead(false);
        n.setCreatedAt(1000L);

        assertEquals("id-1", n.getId());
        assertEquals("user1", n.getUserId());
        assertEquals("Test", n.getTitle());
        assertEquals("Desc", n.getDescription());
        assertEquals(NotificationType.COURSE_ALERT, n.getType());
        assertFalse(n.getIsRead());
        assertEquals(1000L, n.getCreatedAt());
    }

    @Test
    void isRead_shouldDefaultToFalse() {
        InAppNotification n = new InAppNotification();
        // isRead defaults to Boolean.FALSE per model definition
        assertFalse(n.getIsRead());
    }

    @Test
    void setIsRead_shouldUpdateToTrue() {
        InAppNotification n = new InAppNotification();
        n.setIsRead(true);
        assertTrue(n.getIsRead());
    }
}
