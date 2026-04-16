package repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock DynamoDBMapper dynamoDBMapper;
    @InjectMocks NotificationRepository repo;

    @Test
    void save_shouldCallMapperSave() {
        Notification n = buildNotification();
        doNothing().when(dynamoDBMapper).save(n);
        Notification result = repo.save(n);
        assertNotNull(result);
        verify(dynamoDBMapper, times(1)).save(n);
    }

    @Test
    void getById_shouldReturnNotification() {
        Notification n = buildNotification();
        when(dynamoDBMapper.load(Notification.class, "id-1")).thenReturn(n);
        Notification result = repo.getById("id-1");
        assertNotNull(result);
    }

    @Test
    void findById_shouldReturnOptionalPresent() {
        Notification n = buildNotification();
        when(dynamoDBMapper.load(Notification.class, "id-1")).thenReturn(n);
        Optional<Notification> result = repo.findById("id-1");
        assertTrue(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyWhenNull() {
        when(dynamoDBMapper.load(Notification.class, "none")).thenReturn(null);
        Optional<Notification> result = repo.findById("none");
        assertFalse(result.isPresent());
    }

    @Test
    void findByStatus_shouldCallScan() {
        assertDoesNotThrow(() -> repo.findByStatus("PENDING"));
    }

    @Test
    void findByUserId_shouldCallQuery() {
        assertDoesNotThrow(() -> repo.findByUserId("user1"));
    }

    @Test
    void findAll_shouldCallScan() {
        assertDoesNotThrow(() -> repo.findAll());
    }

    @Test
    void delete_shouldCallMapperDelete() {
        Notification n = buildNotification();
        doNothing().when(dynamoDBMapper).delete(n);
        repo.delete(n);
        verify(dynamoDBMapper, times(1)).delete(n);
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
