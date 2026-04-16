package repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.model.InAppNotification;
import org.notification.repository.InAppRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppRepositoryTest {

    @Mock DynamoDBMapper dynamoDBMapper;
    @InjectMocks InAppRepository repo;

    @Test
    void save_shouldCallMapperSave() {
        InAppNotification n = buildNotif("user1");
        doNothing().when(dynamoDBMapper).save(n);
        repo.save(n);
        verify(dynamoDBMapper, times(1)).save(n);
    }

    @Test
    void save_shouldThrowWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
    }

    @Test
    void findById_shouldReturnPresent() {
        InAppNotification n = buildNotif("user1");
        when(dynamoDBMapper.load(InAppNotification.class, "id-1")).thenReturn(n);
        Optional<InAppNotification> result = repo.findById("id-1");
        assertTrue(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyWhenNull() {
        when(dynamoDBMapper.load(InAppNotification.class, "none")).thenReturn(null);
        Optional<InAppNotification> result = repo.findById("none");
        assertFalse(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyForBlankId() {
        Optional<InAppNotification> result = repo.findById("");
        assertFalse(result.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyForNullId() {
        Optional<InAppNotification> result = repo.findById(null);
        assertFalse(result.isPresent());
    }

    @Test
    void getByUserId_shouldReturnEmptyForBlankUserId() {
        List<InAppNotification> result = repo.getByUserId("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getByUserId_shouldReturnEmptyForNullUserId() {
        List<InAppNotification> result = repo.getByUserId(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void delete_shouldCallMapperDelete() {
        InAppNotification n = buildNotif("user1");
        doNothing().when(dynamoDBMapper).delete(n);
        repo.delete(n);
        verify(dynamoDBMapper, times(1)).delete(n);
    }

    @Test
    void delete_shouldNotThrowForNull() {
        assertDoesNotThrow(() -> repo.delete(null));
    }

    @Test
    void deleteById_shouldCallDelete() {
        InAppNotification n = buildNotif("user1");
        when(dynamoDBMapper.load(InAppNotification.class, "id-1")).thenReturn(n);
        doNothing().when(dynamoDBMapper).delete(n);
        repo.deleteById("id-1");
        verify(dynamoDBMapper, times(1)).delete(n);
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
