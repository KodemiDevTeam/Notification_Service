package scheduler;

import org.Notification.model.Notification;
import org.Notification.model.enums.NotificationType;
import org.Notification.repository.NotificationRepository;
import org.Notification.scheduler.NotificationScheduler;
import org.Notification.service.ChannelDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock NotificationRepository repo;
    @Mock ChannelDispatcherService dispatcher;
    @InjectMocks NotificationScheduler scheduler;

    @Test
    void run_shouldDoNothingWhenNoNotifications() {
        ReflectionTestUtils.setField(scheduler, "maxRetry", 3);
        when(repo.findAll()).thenReturn(List.of());

        scheduler.run();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void run_shouldDispatchPendingNotification() {
        ReflectionTestUtils.setField(scheduler, "maxRetry", 3);
        Notification n = buildNotification("PENDING");
        when(repo.findAll()).thenReturn(List.of(n));

        scheduler.run();

        verify(dispatcher, times(1)).dispatch(n);
        assertEquals("SENT", n.getStatus());
    }

    @Test
    void run_shouldSkipNonPendingNotifications() {
        ReflectionTestUtils.setField(scheduler, "maxRetry", 3);
        Notification n = buildNotification("SENT");
        when(repo.findAll()).thenReturn(List.of(n));

        scheduler.run();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void run_shouldMarkFailedWhenMaxRetriesReached() {
        ReflectionTestUtils.setField(scheduler, "maxRetry", 3);
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
        ReflectionTestUtils.setField(scheduler, "maxRetry", 3);
        Notification n = buildNotification("PENDING");
        n.setRetryCount(1);
        when(repo.findAll()).thenReturn(List.of(n));
        doThrow(new RuntimeException("fail")).when(dispatcher).dispatch(n);

        scheduler.run();

        assertEquals(2, n.getRetryCount());
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
