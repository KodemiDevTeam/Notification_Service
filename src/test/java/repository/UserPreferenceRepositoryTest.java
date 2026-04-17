package repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.model.UserPreference;
import org.notification.repository.UserPreferenceRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceRepositoryTest {

    @Mock DynamoDBMapper dynamoDBMapper;
    @InjectMocks UserPreferenceRepository repo;

    @Test
    void save_shouldCallMapperAndReturnPref() {
        UserPreference pref = buildPref("user1");
        doNothing().when(dynamoDBMapper).save(pref);
        UserPreference result = repo.save(pref);
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        verify(dynamoDBMapper, times(1)).save(pref);
    }

    @Test
    void findByUserId_shouldReturnPref() {
        UserPreference pref = buildPref("user1");
        when(dynamoDBMapper.load(UserPreference.class, "user1")).thenReturn(pref);
        UserPreference result = repo.findByUserId("user1");
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
    }

    @Test
    void findByUserId_shouldReturnNullWhenNotFound() {
        when(dynamoDBMapper.load(UserPreference.class, "unknown")).thenReturn(null);
        assertNull(repo.findByUserId("unknown"));
    }

    @Test
    void getByUserId_shouldReturnPref() {
        UserPreference pref = buildPref("user1");
        when(dynamoDBMapper.load(UserPreference.class, "user1")).thenReturn(pref);
        UserPreference result = repo.getByUserId("user1");
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
    }

    @Test
    void getByUserId_shouldReturnNullWhenNotFound() {
        when(dynamoDBMapper.load(UserPreference.class, "unknown")).thenReturn(null);
        assertNull(repo.getByUserId("unknown"));
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
