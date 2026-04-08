package scheduler;

import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.NotificationRepository;
import org.notification.scheduler.NotificationScheduler;
import org.notification.service.ChannelDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock NotificationRepository repo;
    @Mock ChannelDispatcherService dispatcher;

    private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Manually construct since @Value maxRetry can't be injected by @InjectMocks
        scheduler = new NotificationScheduler(repo, dispatcher, 3);
    }

    @Test
    void run_shouldDoNothingWhenNoNotifications() {
        when(repo.findAll()).thenReturn(List.of());

        scheduler.run();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void run_shouldDispatchPendingNotification() {
        Notification n = buildNotification("PENDING");
        when(repo.findAll()).thenReturn(List.of(n));

        scheduler.run();

        verify(dispatcher, times(1)).dispatch(n);
        assertEquals("SENT", n.getStatus());
    }

    @Test
    void run_shouldSkipNonPendingNotifications() {
        Notification n = buildNotification("SENT");
        when(repo.findAll()).thenReturn(List.of(n));

        scheduler.run();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void run_shouldMarkFailedWhenMaxRetriesReached() {
        Notification n = buildNotification("PENDING");
        n.setRetryCount(3);
        when(repo.findAll()).thenReturn(List.of(n));
        doThrow(new RuntimeException("fail")).when(dispatcher).dispatch(n);

        scheduler.run();

        assertEquals("FAILED", n.getStatus());
        verify(repo, atLeastOnce()).save(n);
    }

    @Test
    void run_shouldIncrementRetryCountOnFailure() {
        Notification n = buildNotification("PENDING");
        n.setRetryCount(1);
        when(repo.findAll()).thenReturn(List.of(n));
        doThrow(new RuntimeException("fail")).when(dispatcher).dispatch(n);

        scheduler.run();

        assertEquals(2, n.getRetryCount());
        // status stays PENDING while retries remain
        assertEquals("PENDING", n.getStatus());
    }

    private Notification buildNotification(String status) {
        Notification n = new Notification();
        n.setNotificationId("id-1");
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Desc");
        n.setChannel("EMAIL");
        n.setType(NotificationType.COURSE_ALERT);
        n.setStatus(status);
        n.setRetryCount(0);
        return n;
    }
}
