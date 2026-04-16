package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.model.UserPreference;
import org.notification.repository.UserPreferenceRepository;
import org.notification.service.UserPreferenceService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock UserPreferenceRepository repo;
    @InjectMocks UserPreferenceService service;

    @Test
    void save_shouldReturnSavedPreference() {
        UserPreference pref = buildPref("user1");
        when(repo.save(pref)).thenReturn(pref);
        UserPreference result = service.save(pref);
        assertEquals("user1", result.getUserId());
        verify(repo, times(1)).save(pref);
    }

    @Test
    void getByUserId_shouldReturnPreference() {
        UserPreference pref = buildPref("user1");
        when(repo.findByUserId("user1")).thenReturn(pref);
        UserPreference result = service.getByUserId("user1");
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
    }

    @Test
    void getByUserId_shouldReturnNullWhenNotFound() {
        when(repo.findByUserId("unknown")).thenReturn(null);
        assertNull(service.getByUserId("unknown"));
    }

    private UserPreference buildPref(String userId) {
        UserPreference pref = new UserPreference();
        pref.setUserId(userId);
        pref.setStudentFeedback(true);
        pref.setLiveClassReminder(true);
        pref.setPayoutUpdate(false);
        pref.setStreakUpdate(true);
        pref.setNewEnrollment(true);
        return pref;
    }
}
