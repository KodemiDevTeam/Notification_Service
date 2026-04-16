package model;

import org.junit.jupiter.api.Test;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void defaultValues_shouldBeSetCorrectly() {
        Notification n = new Notification();
        assertEquals("PENDING", n.getStatus());
        assertEquals(0, n.getRetryCount());
        assertFalse(n.getIsRead());
        assertFalse(n.getIsScheduled());
    }

    @Test
    void constants_shouldHaveCorrectValues() {
        assertEquals("notifications", Notification.TABLE_NAME);
        assertEquals("userId-index", Notification.USER_ID_INDEX);
        assertEquals("PENDING", Notification.STATUS_PENDING);
        assertEquals("SENT", Notification.STATUS_SENT);
        assertEquals("FAILED", Notification.STATUS_FAILED);
    }

    @Test
    void getMessage_shouldReturnDescription() {
        Notification n = new Notification();
        n.setDescription("Hello World");
        assertEquals("Hello World", n.getMessage());
    }

    @Test
    void getMessage_shouldReturnEmptyWhenDescriptionIsNull() {
        Notification n = new Notification();
        n.setDescription(null);
        assertEquals("", n.getMessage());
    }

    @Test
    void settersAndGetters_allFields() {
        Notification n = new Notification();
        n.setNotificationId("id-1");
        n.setUserId("user1");
        n.setTitle("Title");
        n.setDescription("Desc");
        n.setType(NotificationType.COURSE_ALERT);
        n.setChannel("EMAIL");
        n.setEmail("a@b.com");
        n.setPhoneNumber("+91999");
        n.setDeviceToken("token-abc");
        n.setIsRead(true);
        n.setIsScheduled(true);
        n.setScheduledTime(1000L);
        n.setCreatedAt(2000L);
        n.setStatus("SENT");
        n.setRetryCount(2);
        n.setRedirectUrl("https://example.com");

        assertEquals("id-1", n.getNotificationId());
        assertEquals("user1", n.getUserId());
        assertEquals("Title", n.getTitle());
        assertEquals("Desc", n.getDescription());
        assertEquals(NotificationType.COURSE_ALERT, n.getType());
        assertEquals("EMAIL", n.getChannel());
        assertEquals("a@b.com", n.getEmail());
        assertEquals("+91999", n.getPhoneNumber());
        assertEquals("token-abc", n.getDeviceToken());
        assertTrue(n.getIsRead());
        assertTrue(n.getIsScheduled());
        assertEquals(1000L, n.getScheduledTime());
        assertEquals(2000L, n.getCreatedAt());
        assertEquals("SENT", n.getStatus());
        assertEquals(2, n.getRetryCount());
        assertEquals("https://example.com", n.getRedirectUrl());
    }

    @Test
    void equalsAndHashCode_shouldWorkForSameObject() {
        Notification n = new Notification();
        n.setNotificationId("id-1");
        n.setUserId("user1");
        assertEquals(n, n);
        assertEquals(n.hashCode(), n.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        Notification n = new Notification();
        n.setNotificationId("id-1");
        assertNotNull(n.toString());
    }

    @Test
    void allNotificationTypes_shouldBeSettable() {
        Notification n = new Notification();
        for (NotificationType type : NotificationType.values()) {
            n.setType(type);
            assertEquals(type, n.getType());
        }
    }
}
