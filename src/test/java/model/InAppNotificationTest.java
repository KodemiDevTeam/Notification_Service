package model;

import org.junit.jupiter.api.Test;
import org.notification.model.InAppNotification;
import org.notification.model.enums.NotificationType;

import static org.junit.jupiter.api.Assertions.*;

class InAppNotificationTest {

    @Test
    void defaultValues_shouldBeSetCorrectly() {
        InAppNotification n = new InAppNotification();
        assertFalse(n.getIsRead());
    }

    @Test
    void constants_shouldHaveCorrectValues() {
        assertEquals("inapp_notifications", InAppNotification.TABLE_NAME);
        assertEquals("userId-index", InAppNotification.USER_ID_INDEX);
    }

    @Test
    void settersAndGetters_allFields() {
        InAppNotification n = new InAppNotification();
        n.setId("id-1");
        n.setUserId("user1");
        n.setTitle("Title");
        n.setDescription("Desc");
        n.setType(NotificationType.STREAK_ALERT);
        n.setIsRead(true);
        n.setCreatedAt(1000L);
        n.setRedirectUrl("https://example.com");

        assertEquals("id-1", n.getId());
        assertEquals("user1", n.getUserId());
        assertEquals("Title", n.getTitle());
        assertEquals("Desc", n.getDescription());
        assertEquals(NotificationType.STREAK_ALERT, n.getType());
        assertTrue(n.getIsRead());
        assertEquals(1000L, n.getCreatedAt());
        assertEquals("https://example.com", n.getRedirectUrl());
    }

    @Test
    void setIsRead_shouldUpdateValue() {
        InAppNotification n = new InAppNotification();
        assertFalse(n.getIsRead());
        n.setIsRead(true);
        assertTrue(n.getIsRead());
        n.setIsRead(false);
        assertFalse(n.getIsRead());
    }

    @Test
    void equalsAndHashCode_shouldWorkForSameObject() {
        InAppNotification n = new InAppNotification();
        n.setId("id-1");
        assertEquals(n, n);
        assertEquals(n.hashCode(), n.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        InAppNotification n = new InAppNotification();
        n.setId("id-1");
        assertNotNull(n.toString());
    }

    @Test
    void allNotificationTypes_shouldBeSettable() {
        InAppNotification n = new InAppNotification();
        for (NotificationType type : NotificationType.values()) {
            n.setType(type);
            assertEquals(type, n.getType());
        }
    }
}
