package org.notification.scheduler;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.exception.PermanentFailureException;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.SendMode;
import org.notification.repository.NotificationRepository;
import org.notification.service.NotificationDispatcher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationDispatcher dispatcher;

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @InjectMocks
    private NotificationScheduler scheduler;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);
        ReflectionTestUtils.setField(scheduler, "defaultTimezone", "Asia/Kolkata");
    }

    @Test
    void testProcessPendingNotifications_Success() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SEND_NOW);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING)))
                .thenReturn(List.of(n));
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED)))
                .thenReturn(List.of());
        
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);

        scheduler.processPendingNotifications();

        verify(dispatcher, times(1)).dispatch(n);
        verify(repository, times(1)).save(n);
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }

    @Test
    void testProcessPendingNotifications_LockFailed() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(false);

        scheduler.processPendingNotifications();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void testProcessPendingNotifications_PermanentFailure() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SEND_NOW);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);

        doThrow(new PermanentFailureException("Bad number")).when(dispatcher).dispatch(n);

        scheduler.processPendingNotifications();

        verify(repository, times(1)).save(n);
        assertEquals(NotificationStatus.FAILED, n.getStatus());
    }

    @Test
    void testProcessPendingNotifications_TemporaryFailure() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SEND_NOW);
        n.setRetryCount(0);
        n.setMaxRetries(3);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);

        doThrow(new RuntimeException("Timeout")).when(dispatcher).dispatch(n);

        scheduler.processPendingNotifications();

        verify(repository, times(1)).save(n);
        assertEquals(NotificationStatus.RETRY_SCHEDULED, n.getStatus());
        assertEquals(1, n.getRetryCount());
    }

    @Test
    void testProcessPendingNotifications_MaxRetriesExceeded() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SEND_NOW);
        n.setRetryCount(3); // Equal to max
        n.setMaxRetries(3);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);

        doThrow(new RuntimeException("Timeout")).when(dispatcher).dispatch(n);

        scheduler.processPendingNotifications();

        verify(repository, times(1)).save(n);
        assertEquals(NotificationStatus.FAILED, n.getStatus());
    }

    @Test
    void testScheduleNextRecurrenceIfApplicable() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.DAILY_MORNING); // triggers recurrence
        n.setScheduledAt(1716435000000L); // Some valid timestamp
        n.setBatchId("batch1");
        n.setUserId("user1");
        n.setChannel(org.notification.model.enums.NotificationChannel.EMAIL);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);
        when(repository.existsByRecurrenceKey(anyString())).thenReturn(false);

        scheduler.processPendingNotifications();

        verify(dynamoDBMapper, times(1)).save(notificationCaptor.capture());
        Notification next = notificationCaptor.getValue();
        assertEquals(SendMode.DAILY_MORNING, next.getSendMode());
        assertEquals("batch1", next.getBatchId());
    }

    @Test
    void testProcessPendingNotifications_ProviderDisabled_MarksSkipped() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.SEND_NOW);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED))).thenReturn(List.of());
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);

        doThrow(new org.notification.exception.ProviderDisabledException("EMAIL_PROVIDER_DISABLED"))
                .when(dispatcher).dispatch(n);

        scheduler.processPendingNotifications();

        verify(repository, times(1)).save(n);
        assertEquals(NotificationStatus.SKIPPED, n.getStatus());
    }

    @Test
    void testProcessPendingNotifications_LockException_Skips() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED))).thenReturn(List.of());
        when(repository.acquireLock(anyString(), any(), any())).thenThrow(new RuntimeException("DynamoDB error"));

        scheduler.processPendingNotifications();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void testProcessPendingNotifications_EmptyList_DoesNothing() {
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of());
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED))).thenReturn(List.of());

        scheduler.processPendingNotifications();

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void testScheduleNextRecurrence_AlreadyExists_DoesNotSave() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setStatus(NotificationStatus.PENDING);
        n.setSendMode(SendMode.DAILY_EVENING);
        n.setScheduledAt(1716435000000L);
        n.setBatchId("batch1");
        n.setUserId("user1");
        n.setChannel(org.notification.model.enums.NotificationChannel.IN_APP);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING))).thenReturn(List.of(n));
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED))).thenReturn(List.of());
        when(repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING)).thenReturn(true);
        when(repository.existsByRecurrenceKey(anyString())).thenReturn(true);

        scheduler.processPendingNotifications();

        verify(dynamoDBMapper, never()).save(any());
    }

    @Test
    void testProcessPendingNotifications_Sorting() {
        Notification n1 = new Notification();
        n1.setNotificationId("n1");
        n1.setStatus(NotificationStatus.PENDING);
        n1.setScheduledAt(2000L);

        Notification n2 = new Notification();
        n2.setNotificationId("n2");
        n2.setStatus(NotificationStatus.RETRY_SCHEDULED);
        n2.setScheduledAt(1000L);

        Notification n3 = new Notification();
        n3.setNotificationId("n3");
        n3.setStatus(NotificationStatus.PENDING);
        n3.setScheduledAt(null);

        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.PENDING)))
                .thenReturn(Arrays.asList(n1, n3));
        when(repository.findDueNotifications(anyLong(), anyInt(), eq(NotificationStatus.RETRY_SCHEDULED)))
                .thenReturn(List.of(n2));

        when(repository.acquireLock(anyString(), any(), any())).thenReturn(true);

        scheduler.processPendingNotifications();

        org.mockito.InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).dispatch(n3);
        inOrder.verify(dispatcher).dispatch(n2);
        inOrder.verify(dispatcher).dispatch(n1);
    }
}

