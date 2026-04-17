package model;

import org.junit.jupiter.api.Test;
import org.notification.model.UserPreference;

import static org.junit.jupiter.api.Assertions.*;

class UserPreferenceTest {

    @Test
    void settersAndGetters_allFields() {
        UserPreference pref = new UserPreference();
        pref.setUserId("user1");
        pref.setStudentFeedback(true);
        pref.setLiveClassReminder(true);
        pref.setPayoutUpdate(false);
        pref.setStreakUpdate(true);
        pref.setNewEnrollment(false);

        assertEquals("user1", pref.getUserId());
        assertTrue(pref.getStudentFeedback());
        assertTrue(pref.getLiveClassReminder());
        assertFalse(pref.getPayoutUpdate());
        assertTrue(pref.getStreakUpdate());
        assertFalse(pref.getNewEnrollment());
    }

    @Test
    void defaultValues_shouldBeNull() {
        UserPreference pref = new UserPreference();
        assertNull(pref.getUserId());
        assertNull(pref.getStudentFeedback());
        assertNull(pref.getLiveClassReminder());
        assertNull(pref.getPayoutUpdate());
        assertNull(pref.getStreakUpdate());
        assertNull(pref.getNewEnrollment());
    }

    @Test
    void equals_shouldReturnTrueForSameObject() {
        UserPreference p = buildFull();
        assertEquals(p, p);
    }

    @Test
    void equals_shouldReturnTrueForEqualObjects() {
        assertEquals(buildFull(), buildFull());
    }

    @Test
    void equals_shouldReturnFalseForDifferentUserId() {
        UserPreference p1 = buildFull();
        UserPreference p2 = buildFull();
        p2.setUserId("different");
        assertNotEquals(p1, p2);
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
        UserPreference p1 = new UserPreference();
        UserPreference p2 = new UserPreference();
        assertEquals(p1, p2);
    }

    @Test
    void hashCode_shouldBeEqualForEqualObjects() {
        assertEquals(buildFull().hashCode(), buildFull().hashCode());
    }

    @Test
    void hashCode_shouldDifferForDifferentObjects() {
        UserPreference p1 = buildFull();
        UserPreference p2 = buildFull();
        p2.setUserId("different");
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        assertNotNull(buildFull().toString());
    }

    private UserPreference buildFull() {
        UserPreference pref = new UserPreference();
        pref.setUserId("user1");
        pref.setStudentFeedback(true);
        pref.setLiveClassReminder(true);
        pref.setPayoutUpdate(false);
        pref.setStreakUpdate(true);
        pref.setNewEnrollment(false);
        return pref;
    }
}
