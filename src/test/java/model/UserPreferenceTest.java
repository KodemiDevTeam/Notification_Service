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
    void equalsAndHashCode_shouldWorkForSameObject() {
        UserPreference pref = new UserPreference();
        pref.setUserId("user1");
        assertEquals(pref, pref);
        assertEquals(pref.hashCode(), pref.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        UserPreference pref = new UserPreference();
        pref.setUserId("user1");
        assertNotNull(pref.toString());
    }

    @Test
    void allPreferences_canBeToggled() {
        UserPreference pref = new UserPreference();

        pref.setStudentFeedback(true);
        assertTrue(pref.getStudentFeedback());
        pref.setStudentFeedback(false);
        assertFalse(pref.getStudentFeedback());

        pref.setLiveClassReminder(true);
        assertTrue(pref.getLiveClassReminder());

        pref.setPayoutUpdate(true);
        assertTrue(pref.getPayoutUpdate());

        pref.setStreakUpdate(true);
        assertTrue(pref.getStreakUpdate());

        pref.setNewEnrollment(true);
        assertTrue(pref.getNewEnrollment());
    }
}
