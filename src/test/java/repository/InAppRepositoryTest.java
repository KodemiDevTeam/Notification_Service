package repository;

import org.notification.model.InAppNotification;
import org.notification.repository.InAppRepository;
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
class InAppRepositoryTest {

    @Mock InAppRepository repo;

    @Test
    void save_shouldCallSave() {
        InAppNotification n = buildNotif("user1");
        doNothing().when(repo).save(n);
        repo.save(n);
        verify(repo, times(1)).save(n);
    }

    @Test
    void getByUserId_shouldReturnList() {
        InAppNotification n = buildNotif("user1");
        when(repo.getByUserId("user1")).thenReturn(List.of(n));
        List<InAppNotification> result = repo.getByUserId("user1");
        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUserId());
    }

    @Test
    void getByUserId_shouldReturnEmptyForUnknown() {
        when(repo.getByUserId("unknown")).thenReturn(List.of());
        assertTrue(repo.getByUserId("unknown").isEmpty());
    }

    @Test
    void getUnread_shouldReturnUnreadNotifications() {
        InAppNotification n = buildNotif("user1");
        when(repo.getUnread("user1")).thenReturn(List.of(n));
        List<InAppNotification> result = repo.getUnread("user1");
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    void getUnread_shouldReturnEmptyWhenAllRead() {
        when(repo.getUnread("user1")).thenReturn(List.of());
        assertTrue(repo.getUnread("user1").isEmpty());
    }

    @Test
    void findById_shouldReturnNotification() {
        String id = UUID.randomUUID().toString();
        InAppNotification n = buildNotif("user1");
        n.setId(id);
        when(repo.findById(id)).thenReturn(Optional.of(n));
        assertTrue(repo.findById(id).isPresent());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(repo.findById("none")).thenReturn(Optional.empty());
        assertFalse(repo.findById("none").isPresent());
    }

    @Test
    void deleteById_shouldCallDelete() {
        doNothing().when(repo).deleteById("id-1");
        repo.deleteById("id-1");
        verify(repo, times(1)).deleteById("id-1");
    }

    @Test
    void delete_shouldCallDelete() {
        InAppNotification n = buildNotif("user1");
        doNothing().when(repo).delete(n);
        repo.delete(n);
        verify(repo, times(1)).delete(n);
    }

    private InAppNotification buildNotif(String userId) {
        InAppNotification n = new InAppNotification();
        n.setId(UUID.randomUUID().toString());
        n.setUserId(userId);
        n.setTitle("Test");
        n.setDescription("Desc");
        n.setIsRead(false);
        n.setCreatedAt(System.currentTimeMillis());
        return n;
    }
}
