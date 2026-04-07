package channel;

import org.Notification.channel.InAppChannel;
import org.Notification.model.InAppNotification;
import org.Notification.model.Notification;
import org.Notification.repository.InAppRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppChannelTest {

    @Mock InAppRepository repo;
    @InjectMocks InAppChannel inAppChannel;

    @Test
    void getChannelName_shouldReturnIN_APP() {
        assertEquals("IN_APP", inAppChannel.getChannelName());
    }

    @Test
    void send_shouldSaveToRepo() {
        Notification n = new Notification();
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Desc");

        inAppChannel.send(n);

        verify(repo, times(1)).save(any(InAppNotification.class));
    }

    @Test
    void send_shouldThrowWhenUserIdIsNull() {
        Notification n = new Notification();
        n.setUserId(null);

        assertThrows(RuntimeException.class, () -> inAppChannel.send(n));
        verify(repo, never()).save(any());
    }

    @Test
    void send_shouldThrowWhenUserIdIsEmpty() {
        Notification n = new Notification();
        n.setUserId("");

        assertThrows(RuntimeException.class, () -> inAppChannel.send(n));
        verify(repo, never()).save(any());
    }

    @Test
    void send_shouldSetCorrectFields() {
        Notification n = new Notification();
        n.setUserId("user1");
        n.setTitle("Hello");
        n.setDescription("World");

        inAppChannel.send(n);

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(repo).save(captor.capture());

        InAppNotification saved = captor.getValue();
        assertEquals("user1", saved.getUserId());
        assertFalse(saved.getIsRead());
        assertNotNull(saved.getId());
        assertTrue(saved.getCreatedAt() > 0);
    }
}
