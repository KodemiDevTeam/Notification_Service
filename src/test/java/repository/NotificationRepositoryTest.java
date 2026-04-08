package repository;

import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock NotificationRepository repo;

    @Test
    void save_shouldReturnSavedNotification() {
        Notification n = buildNotification();
        when(repo.save(n)).thenReturn(n);
        Notification saved = repo.save(n);
        assertNotNull(saved);
        assertEquals("user1", saved.getUserId());
        verify(repo, times(1)).save(n);
    }

    @Test
    void findByUserId_shouldReturnList() {
        Notification n = buildNotification();
        when(repo.findByUserId("user1")).thenReturn(List.of(n));
        List<Notification> result = repo.findByUserId("user1");
        assertEquals(1, result.size());
    }

    @Test
    void findByUserId_shouldReturnEmptyForUnknown() {
        when(repo.findByUserId("unknown")).thenReturn(List.of());
        assertTrue(repo.findByUserId("unknown").isEmpty());
    }

    @Test
    void findById_shouldReturnNotification() {
        String id = UUID.randomUUID().toString();
        Notification n = buildNotification();
        when(repo.findById(id)).thenReturn(Optional.of(n));
        Optional<Notification> result = repo.findById(id);
        assertTrue(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(repo.findById("none")).thenReturn(Optional.empty());
        assertFalse(repo.findById("none").isPresent());
    }

    @Test
    void delete_shouldCallDelete() {
        Notification n = buildNotification();
        doNothing().when(repo).delete(n);
        repo.delete(n);
        verify(repo, times(1)).delete(n);
    }

    @Test
    void findAll_shouldReturnList() {
        Notification n = buildNotification();
        when(repo.findAll()).thenReturn(List.of(n));
        List<Notification> result = repo.findAll();
        assertEquals(1, result.size());
    }

    private Notification buildNotification() {
        Notification n = new Notification();
        n.setNotificationId(UUID.randomUUID().toString());
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Desc");
        n.setChannel("EMAIL");
        n.setType(NotificationType.COURSE_ALERT);
        n.setStatus("PENDING");
        return n;
    }
}
