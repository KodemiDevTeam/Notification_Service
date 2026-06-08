package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.client.UserClient;
import org.notification.dto.request.BroadcastNotificationRequest;
import org.notification.dto.request.NotificationRequest;
import org.notification.dto.request.ScheduledNotificationRequest;
import org.notification.dto.response.BroadcastNotificationResponse;
import org.notification.dto.response.NotificationResponse;
import org.notification.dto.response.UserNotificationTargetDTO;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationPriority;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.NotificationType;
import org.notification.model.enums.SendMode;
import org.notification.repository.NotificationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "defaultTimezone", "Asia/Kolkata");
        ReflectionTestUtils.setField(notificationService, "defaultMorningTime", "09:00");
        ReflectionTestUtils.setField(notificationService, "defaultEveningTime", "18:00");
        ReflectionTestUtils.setField(notificationService, "defaultMaxRetries", 3);
    }

    @Test
    void testBroadcast_SendNow_Success() {
        // Arrange
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setSendMode(SendMode.SEND_NOW);
        req.setChannels(Arrays.asList(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP));
        req.setTitle("Test Title");
        req.setMessage("Test Message");
        req.setType(NotificationType.GENERAL);
        req.setPriority(NotificationPriority.HIGH);

        UserNotificationTargetDTO target1 = new UserNotificationTargetDTO();
        target1.setUserId("user1");
        target1.setEmail("test1@example.com");
        target1.setPhoneNumber("+919000000001");

        UserNotificationTargetDTO target2 = new UserNotificationTargetDTO();
        target2.setUserId("user2");
        target2.setEmail("test2@example.com");
        target2.setPhoneNumber("+919000000002");

        // Duplicate target to test deduplication
        UserNotificationTargetDTO target3 = new UserNotificationTargetDTO();
        target3.setUserId("user2"); // Same user ID
        target3.setEmail("test2@example.com");
        target3.setPhoneNumber("+919000000002");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(Arrays.asList(target1, target2, target3));

        // Act
        BroadcastNotificationResponse response = notificationService.broadcast(req);

        // Assert
        assertEquals(2, response.getTotalUsers());
        assertEquals(2, response.getEmailCreated());
        assertEquals(2, response.getSmsCreated());
        assertEquals(2, response.getInAppCreated());
        assertEquals(6, response.getTotalNotificationsCreated());
        assertNotNull(response.getBatchId());

        verify(repository, times(6)).saveIdempotent(notificationCaptor.capture());
        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertEquals(NotificationStatus.PENDING, savedNotifications.get(0).getStatus());
        assertEquals(NotificationPriority.HIGH, savedNotifications.get(0).getPriority());
    }

    @Test
    void testBroadcast_DailyMorning_Success() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("TRAINER");
        req.setSendMode(SendMode.DAILY_MORNING);
        req.setChannels(List.of(NotificationChannel.EMAIL));
        req.setType(NotificationType.GENERAL);

        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("user1");
        target.setEmail("test@example.com");

        when(userClient.getUsersForNotification("TRAINER")).thenReturn(List.of(target));
        when(repository.existsByRecurrenceKey(anyString())).thenReturn(false);

        BroadcastNotificationResponse response = notificationService.broadcast(req);

        assertEquals(1, response.getEmailCreated());
        verify(repository, times(1)).saveIdempotent(any(Notification.class));
    }

    @Test
    void testSendImmediate() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setChannel(NotificationChannel.SMS);
        req.setPhoneNumber("+919999999999");
        req.setTitle("Immediate");
        req.setType(NotificationType.GENERAL);
        
        notificationService.sendImmediate(req);

        verify(repository, times(1)).saveIdempotent(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals(SendMode.SEND_NOW, saved.getSendMode());
        assertEquals("+919999999999", saved.getRecipientPhone());
    }

    @Test
    void testSchedule() {
        ScheduledNotificationRequest req = new ScheduledNotificationRequest();
        req.setUserId("u1");
        req.setChannel(NotificationChannel.EMAIL);
        req.setEmail("test@example.com");
        req.setScheduledTime(1700000000000L);
        
        notificationService.schedule(req);

        verify(repository, times(1)).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals(SendMode.SCHEDULED, saved.getSendMode());
        assertEquals(1700000000000L, saved.getScheduledAt());
    }

    @Test
    void testCancelBatch() {
        Notification n1 = new Notification();
        n1.setStatus(NotificationStatus.PENDING);
        Notification n2 = new Notification();
        n2.setStatus(NotificationStatus.RETRY_SCHEDULED);
        Notification n3 = new Notification();
        n3.setStatus(NotificationStatus.SENT); // Should not be cancelled

        when(repository.findByBatchId("batch1")).thenReturn(Arrays.asList(n1, n2, n3));

        notificationService.cancelBatch("batch1");

        verify(repository, times(2)).save(notificationCaptor.capture());
        assertEquals(NotificationStatus.CANCELLED, notificationCaptor.getAllValues().get(0).getStatus());
        assertEquals(NotificationStatus.CANCELLED, notificationCaptor.getAllValues().get(1).getStatus());
    }

    @Test
    void testSendNowBatch() {
        Notification n1 = new Notification();
        n1.setStatus(NotificationStatus.PENDING);
        
        when(repository.findByBatchId("batch1")).thenReturn(List.of(n1));

        notificationService.sendNowBatch("batch1");

        verify(repository, times(1)).save(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().getScheduledAt() <= System.currentTimeMillis());
    }

    @Test
    void testRescheduleBatch() {
        Notification n1 = new Notification();
        n1.setStatus(NotificationStatus.PENDING);
        
        when(repository.findByBatchId("batch1")).thenReturn(List.of(n1));

        notificationService.rescheduleBatch("batch1", 1800000000000L);

        verify(repository, times(1)).save(notificationCaptor.capture());
        assertEquals(1800000000000L, notificationCaptor.getValue().getScheduledAt());
    }

    @Test
    void testGetUserNotifications() {
        Notification n1 = new Notification();
        n1.setNotificationId("n1");
        n1.setUserId("u1");

        when(repository.findByUserId("u1")).thenReturn(List.of(n1));

        List<NotificationResponse> result = notificationService.getUserNotifications("u1");
        assertEquals(1, result.size());
        assertEquals("n1", result.get(0).getNotificationId());
    }

    @Test
    void testMarkAsRead() {
        Notification n1 = new Notification();
        n1.setNotificationId("n1");
        n1.setRead(false);

        when(repository.findById("n1")).thenReturn(n1);

        notificationService.markAsRead("n1");

        verify(repository, times(1)).save(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().getRead());
    }

    @Test
    void testMarkAllAsRead() {
        Notification n1 = new Notification();
        n1.setRead(false);
        Notification n2 = new Notification();
        n2.setRead(true); // Should not be saved again

        when(repository.findByUserId("u1")).thenReturn(Arrays.asList(n1, n2));

        notificationService.markAllAsRead("u1");

        verify(repository, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendInternal_NoChannels_HasChannel() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannel(NotificationChannel.EMAIL);
        req.setEmail("test@example.com");
        req.setTitle("T");
        req.setMessage("M");

        when(repository.saveIdempotent(any())).thenReturn(true);

        notificationService.sendInternal(req);

        assertEquals(List.of("EMAIL"), req.getChannels());
        verify(repository, times(1)).saveIdempotent(any());
    }

    @Test
    void testSendInternal_NoChannels_NoChannel() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setTitle("T");
        req.setMessage("M");

        when(repository.saveIdempotent(any())).thenReturn(true);

        notificationService.sendInternal(req);

        assertEquals(List.of("IN_APP"), req.getChannels());
        verify(repository, times(1)).saveIdempotent(any());
    }

    @Test
    void testSendInternal_FetchContact_Success() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannels(Arrays.asList("EMAIL", "SMS"));
        req.setTitle("T");
        req.setMessage("M");

        UserNotificationTargetDTO contact = new UserNotificationTargetDTO();
        contact.setEmail("fetched@example.com");
        contact.setPhoneNumber("+918888888888");

        when(userClient.getUserContact("u1")).thenReturn(contact);
        when(repository.saveIdempotent(any())).thenReturn(true);

        notificationService.sendInternal(req);

        assertEquals("fetched@example.com", req.getEmail());
        assertEquals("+918888888888", req.getPhoneNumber());
        verify(repository, times(2)).saveIdempotent(any());
    }

    @Test
    void testSendInternal_FetchContact_Exception() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannels(Arrays.asList("EMAIL"));
        req.setTitle("T");
        req.setMessage("M");

        when(userClient.getUserContact("u1")).thenThrow(new RuntimeException("API error"));

        notificationService.sendInternal(req);

        // It should log and continue, skipping save since email is still null
        verify(repository, never()).saveIdempotent(any());
    }

    @Test
    void testSendInternal_SkipMissingInfo() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannels(Arrays.asList("EMAIL", "SMS", "IN_APP"));
        req.setTitle("T");
        req.setMessage("M");

        // No contact found or returned
        when(userClient.getUserContact("u1")).thenReturn(null);
        when(repository.saveIdempotent(any())).thenReturn(true);

        notificationService.sendInternal(req);

        // EMAIL and SMS are skipped, only IN_APP is processed
        verify(repository, times(1)).saveIdempotent(notificationCaptor.capture());
        assertEquals(NotificationChannel.IN_APP, notificationCaptor.getValue().getChannel());
    }

    @Test
    void testSendInternal_InvalidChannel() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannels(Arrays.asList("INVALID_CHANNEL", "IN_APP"));
        req.setTitle("T");
        req.setMessage("M");

        when(repository.saveIdempotent(any())).thenReturn(true);

        // INVALID_CHANNEL throws IllegalArgumentException in valueOf, which is caught. IN_APP should still run.
        notificationService.sendInternal(req);

        verify(repository, times(1)).saveIdempotent(any());
    }

    @Test
    void testSendInternal_DuplicateSave() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId("u1");
        req.setType(NotificationType.GENERAL);
        req.setChannels(List.of("IN_APP"));
        req.setTitle("T");
        req.setMessage("M");

        when(repository.saveIdempotent(any())).thenReturn(false);

        notificationService.sendInternal(req);

        verify(repository, times(1)).saveIdempotent(any());
    }

    @Test
    void testBroadcast_Scheduled_ValidAndInvalid() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setType(NotificationType.GENERAL);
        req.setSendMode(SendMode.SCHEDULED);
        req.setChannels(List.of(NotificationChannel.IN_APP));
        req.setTitle("Test");
        req.setMessage("Test");
        req.setScheduledAt("2026-06-05T12:00:00");

        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("u1");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(List.of(target));
        when(repository.saveIdempotent(any())).thenReturn(true);

        BroadcastNotificationResponse response = notificationService.broadcast(req);
        assertEquals(1, response.getTotalNotificationsCreated());

        // Now test with invalid scheduledAt format
        req.setScheduledAt("invalid-date-format");
        response = notificationService.broadcast(req);
        assertEquals(1, response.getTotalNotificationsCreated());
    }

    @Test
    void testBroadcast_CustomRecurring_ValidAndInvalid() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setType(NotificationType.GENERAL);
        req.setSendMode(SendMode.CUSTOM_RECURRING);
        req.setChannels(List.of(NotificationChannel.IN_APP));
        req.setTitle("Test");
        req.setMessage("Test");
        req.setScheduledAt("2026-06-05T12:00:00");

        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("u1");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(List.of(target));
        when(repository.saveIdempotent(any())).thenReturn(true);

        BroadcastNotificationResponse response = notificationService.broadcast(req);
        assertEquals(1, response.getTotalNotificationsCreated());

        // Now test with invalid scheduledAt format
        req.setScheduledAt("invalid-date-format");
        response = notificationService.broadcast(req);
        assertEquals(1, response.getTotalNotificationsCreated());
    }

    @Test
    void testBroadcast_Evening_And_MorningEvening() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setType(NotificationType.GENERAL);
        req.setSendMode(SendMode.DAILY_EVENING);
        req.setChannels(List.of(NotificationChannel.IN_APP));
        req.setTitle("Test");
        req.setMessage("Test");

        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("u1");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(List.of(target));
        when(repository.saveIdempotent(any())).thenReturn(true);
        when(repository.existsByRecurrenceKey(anyString())).thenReturn(false);

        BroadcastNotificationResponse response = notificationService.broadcast(req);
        assertEquals(1, response.getTotalNotificationsCreated());

        // Test with DAILY_MORNING_EVENING
        req.setSendMode(SendMode.DAILY_MORNING_EVENING);
        response = notificationService.broadcast(req);
        assertEquals(2, response.getTotalNotificationsCreated());
    }

    @Test
    void testBroadcast_DeduplicationSkippedInfo() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setType(NotificationType.GENERAL);
        req.setSendMode(SendMode.SEND_NOW);
        req.setChannels(Arrays.asList(NotificationChannel.EMAIL, NotificationChannel.SMS));
        req.setTitle("Test");
        req.setMessage("Test");

        // Target with missing email and phone
        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("u1");
        target.setEmail("");
        target.setPhoneNumber("");

        // Another target with same emails and phones (to test processedEmails/processedPhones set deduplication)
        UserNotificationTargetDTO target2 = new UserNotificationTargetDTO();
        target2.setUserId("u2");
        target2.setEmail("dup@example.com");
        target2.setPhoneNumber("12345");

        UserNotificationTargetDTO target3 = new UserNotificationTargetDTO();
        target3.setUserId("u3");
        target3.setEmail("dup@example.com");
        target3.setPhoneNumber("12345");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(Arrays.asList(target, target2, target3));
        when(repository.saveIdempotent(any())).thenReturn(true);

        BroadcastNotificationResponse response = notificationService.broadcast(req);
        assertEquals(1, response.getSkippedEmailMissing());
        assertEquals(1, response.getSkippedPhoneMissing());
        assertEquals(2, response.getTotalNotificationsCreated()); // 1 email and 1 SMS for target2, target3 skipped
    }

    @Test
    void testTryCreateRecurring_AlreadyExists() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTargetRole("LEARNER");
        req.setType(NotificationType.GENERAL);
        req.setSendMode(SendMode.DAILY_MORNING);
        req.setChannels(List.of(NotificationChannel.IN_APP));

        UserNotificationTargetDTO target = new UserNotificationTargetDTO();
        target.setUserId("u1");

        when(userClient.getUsersForNotification("LEARNER")).thenReturn(List.of(target));
        when(repository.existsByRecurrenceKey(anyString())).thenReturn(true); // Exists!

        BroadcastNotificationResponse response = notificationService.broadcast(req);
        assertEquals(0, response.getTotalNotificationsCreated());
    }

    @Test
    void testSendNowBatch_MixedStatus() {
        Notification n1 = new Notification();
        n1.setStatus(NotificationStatus.PENDING);
        Notification n2 = new Notification();
        n2.setStatus(NotificationStatus.SENT); // Should be ignored

        when(repository.findByBatchId("batch1")).thenReturn(Arrays.asList(n1, n2));

        notificationService.sendNowBatch("batch1");

        verify(repository, times(1)).save(n1);
        verify(repository, never()).save(n2);
    }

    @Test
    void testRescheduleBatch_MixedStatus() {
        Notification n1 = new Notification();
        n1.setStatus(NotificationStatus.PENDING);
        Notification n2 = new Notification();
        n2.setStatus(NotificationStatus.SENT); // Should be ignored

        when(repository.findByBatchId("batch1")).thenReturn(Arrays.asList(n1, n2));

        notificationService.rescheduleBatch("batch1", 1000L);

        verify(repository, times(1)).save(n1);
        verify(repository, never()).save(n2);
    }

    @Test
    void testGetUnreadCount() {
        Notification n1 = new Notification();
        n1.setRead(false);
        Notification n2 = new Notification();
        n2.setRead(true);

        when(repository.findByUserId("u1")).thenReturn(Arrays.asList(n1, n2));

        long count = notificationService.getUnreadCount("u1");
        assertEquals(1, count);
    }

    @Test
    void testDeleteNotification_Exists() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        when(repository.findById("n1")).thenReturn(n);

        notificationService.deleteNotification("n1");

        verify(repository, times(1)).delete(n);
    }

    @Test
    void testDeleteNotification_NotExist() {
        when(repository.findById("n1")).thenReturn(null);

        notificationService.deleteNotification("n1");

        verify(repository, never()).delete(any());
    }

    @Test
    void testClearUserNotifications() {
        Notification n1 = new Notification();
        Notification n2 = new Notification();
        when(repository.findByUserId("u1")).thenReturn(Arrays.asList(n1, n2));

        notificationService.clearUserNotifications("u1");

        verify(repository, times(2)).delete(any());
    }
}

