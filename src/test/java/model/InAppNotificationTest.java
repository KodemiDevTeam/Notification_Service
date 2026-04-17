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
    void equals_shouldReturnTrueForSameObject() {
        InAppNotification n = buildFull();
        assertEquals(n, n);
    }

    @Test
    void equals_shouldReturnTrueForEqualObjects() {
        InAppNotification n1 = buildFull();
        InAppNotification n2 = buildFull();
        assertEquals(n1, n2);
    }

    @Test
    void equals_shouldReturnFalseForDifferentId() {
        InAppNotification n1 = buildFull();
        InAppNotification n2 = buildFull();
        n2.setId("different");
        assertNotEquals(n1, n2);
    }

    @Test
    void equals_shouldReturnFalseForNull() {
        assertNotEquals(buildFull(), null);
    }

    @Test
    void equals_shouldReturnFalseForDifferentClass() {
        assertNotEquals(buildFull(), "string");
    }

    @Test
    void equals_withNullFields() {
        InAppNotification n1 = new InAppNotification();
        InAppNotification n2 = new InAppNotification();
        assertEquals(n1, n2);
    }

    @Test
    void hashCode_shouldBeEqualForEqualObjects() {
        assertEquals(buildFull().hashCode(), buildFull().hashCode());
    }

    @Test
    void hashCode_shouldDifferForDifferentObjects() {
        InAppNotification n1 = buildFull();
        InAppNotification n2 = buildFull();
        n2.setId("different");
        assertNotEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        assertNotNull(buildFull().toString());
    }

    @Test
    void allNotificationTypes_shouldBeSettable() {
        InAppNotification n = new InAppNotification();
        for (NotificationType type : NotificationType.values()) {
            n.setType(type);
            assertEquals(type, n.getType());
        }
    }

    private InAppNotification buildFull() {
        InAppNotification n = new InAppNotification();
        n.setId("id-1");
        n.setUserId("user1");
        n.setTitle("Title");
        n.setDescription("Desc");
        n.setType(NotificationType.COURSE_ALERT);
        n.setIsRead(false);
        n.setCreatedAt(1000L);
        n.setRedirectUrl("https://example.com");
        return n;
    }
}
