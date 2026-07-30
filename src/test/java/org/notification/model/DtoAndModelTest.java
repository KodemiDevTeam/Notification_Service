package org.notification.model;

import org.junit.jupiter.api.Test;
import org.notification.dto.request.BroadcastNotificationRequest;
import org.notification.dto.request.NotificationRequest;
import org.notification.dto.request.ScheduledNotificationRequest;
import org.notification.dto.response.BroadcastNotificationResponse;
import org.notification.dto.response.NotificationResponse;
import org.notification.dto.response.UserNotificationTargetDTO;
import org.notification.model.enums.NotificationChannel;
import org.notification.model.enums.NotificationStatus;
import org.notification.model.enums.NotificationType;
import org.notification.model.enums.SendMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoAndModelTest {

    @Test
    void testNotificationModelGettersSetters() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setUserId("u1");
        n.setRecipientEmail("u1@example.com");
        n.setRecipientPhone("+1234567890");
        n.setTitle("Title");
        n.setMessage("Message");
        n.setType(NotificationType.SYSTEM_ALERT);
        n.setChannel(NotificationChannel.IN_APP);
        n.setStatus(NotificationStatus.SENT);

        assertEquals("n1", n.getNotificationId());
        assertEquals("u1", n.getUserId());
        assertEquals("u1@example.com", n.getRecipientEmail());
        assertEquals("+1234567890", n.getRecipientPhone());
        assertEquals("Title", n.getTitle());
        assertEquals("Message", n.getMessage());
        assertEquals(NotificationType.SYSTEM_ALERT, n.getType());
        assertEquals(NotificationChannel.IN_APP, n.getChannel());
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }

    @Test
    void testDeviceTokenModelGettersSetters() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("token123");
        dt.setUserId("user1");
        dt.setPlatform("IOS");
        dt.setCreatedAt(1000L);

        assertEquals("token123", dt.getToken());
        assertEquals("user1", dt.getUserId());
        assertEquals("IOS", dt.getPlatform());
        assertEquals(1000L, dt.getCreatedAt());
    }

    @Test
    void testNotificationRequestAndResponse() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId("u1");
        request.setTitle("Title");
        request.setMessage("Msg");
        request.setType(NotificationType.TRANSACTIONAL);
        request.setChannel(NotificationChannel.EMAIL);
        request.setChannels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));

        assertEquals("u1", request.getUserId());
        assertEquals(2, request.getChannels().size());

        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setUserId("u1");
        n.setTitle("Title");
        n.setMessage("Msg");
        n.setRead(false);
        n.setType(NotificationType.TRANSACTIONAL);
        n.setChannel(NotificationChannel.EMAIL);
        n.setStatus(NotificationStatus.SENT);

        NotificationResponse response = NotificationResponse.fromEntity(n);

        assertNotNull(response);
        assertEquals("n1", response.getNotificationId());
        assertFalse(response.getIsRead());
        assertNull(NotificationResponse.fromEntity(null));
    }

    @Test
    void testBroadcastNotificationRequestAndResponse() {
        BroadcastNotificationRequest req = new BroadcastNotificationRequest();
        req.setTitle("Broadcast");
        req.setMessage("Hello all");
        req.setSendMode(SendMode.SEND_NOW);

        assertEquals("Broadcast", req.getTitle());
        assertEquals(SendMode.SEND_NOW, req.getSendMode());

        BroadcastNotificationResponse res = BroadcastNotificationResponse.builder()
                .batchId("b1")
                .totalUsers(100)
                .message("Success")
                .build();

        assertEquals("b1", res.getBatchId());
        assertEquals(100, res.getTotalUsers());
    }

    @Test
    void testUserNotificationTargetDTO() {
        UserNotificationTargetDTO dto = new UserNotificationTargetDTO();
        dto.setUserId("u1");
        dto.setEmail("u1@example.com");
        dto.setPhoneNumber("+123456");

        assertEquals("u1", dto.getUserId());
        assertEquals("u1@example.com", dto.getEmail());
        assertEquals("+123456", dto.getPhoneNumber());
        assertNotNull(dto.toString());
        assertEquals(dto, dto);
        assertNotEquals(dto, null);
    }

    @Test
    void testNotificationFullModel() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setUserId("u1");
        n.setRecipientEmail("email");
        n.setRecipientPhone("phone");
        n.setTitle("t");
        n.setMessage("m");
        n.setType(NotificationType.GENERAL);
        n.setChannel(NotificationChannel.EMAIL);
        n.setStatus(NotificationStatus.PENDING);
        n.setPriority(org.notification.model.enums.NotificationPriority.HIGH);
        n.setSendMode(SendMode.SEND_NOW);
        n.setScheduledAt(1000L);
        n.setCreatedAt(1000L);
        n.setUpdatedAt(1000L);
        n.setSentAt(1000L);
        n.setFailedAt(1000L);
        n.setExpirationTime(1000L);
        n.setRetryCount(1);
        n.setMaxRetries(3);
        n.setNextRetryAt(1000L);
        n.setFailureReason("reason");
        n.setBatchId("b1");
        n.setRecurrenceKey("rk1");
        n.setRecurrenceDate("rd1");
        n.setRecurrenceSlot("rs1");
        n.setRedirectUrl("url");
        n.setReferenceId("ref1");
        n.setIdempotencyKey("ik1");
        n.setRead(true);

        assertEquals("n1", n.getNotificationId());
        assertEquals("u1", n.getUserId());
        assertEquals("email", n.getRecipientEmail());
        assertEquals("phone", n.getRecipientPhone());
        assertEquals("t", n.getTitle());
        assertEquals("m", n.getMessage());
        assertEquals(NotificationType.GENERAL, n.getType());
        assertEquals(NotificationChannel.EMAIL, n.getChannel());
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(org.notification.model.enums.NotificationPriority.HIGH, n.getPriority());
        assertEquals(SendMode.SEND_NOW, n.getSendMode());
        assertEquals(1000L, n.getScheduledAt());
        assertEquals(1000L, n.getCreatedAt());
        assertEquals(1000L, n.getUpdatedAt());
        assertEquals(1000L, n.getSentAt());
        assertEquals(1000L, n.getFailedAt());
        assertEquals(1000L, n.getExpirationTime());
        assertEquals(1, n.getRetryCount());
        assertEquals(3, n.getMaxRetries());
        assertEquals(1000L, n.getNextRetryAt());
        assertEquals("reason", n.getFailureReason());
        assertEquals("b1", n.getBatchId());
        assertEquals("rk1", n.getRecurrenceKey());
        assertEquals("rd1", n.getRecurrenceDate());
        assertEquals("rs1", n.getRecurrenceSlot());
        assertEquals("url", n.getRedirectUrl());
        assertEquals("ref1", n.getReferenceId());
        assertEquals("ik1", n.getIdempotencyKey());
        assertTrue(n.getRead());
    }

    @Test
    void testAllEnums() {
        for (NotificationChannel channel : NotificationChannel.values()) {
            assertNotNull(NotificationChannel.valueOf(channel.name()));
        }
        for (NotificationStatus status : NotificationStatus.values()) {
            assertNotNull(NotificationStatus.valueOf(status.name()));
        }
        for (NotificationType type : NotificationType.values()) {
            assertNotNull(NotificationType.valueOf(type.name()));
        }
        for (SendMode mode : SendMode.values()) {
            assertNotNull(SendMode.valueOf(mode.name()));
        }
        for (org.notification.model.enums.ChannelType ct : org.notification.model.enums.ChannelType.values()) {
            assertNotNull(org.notification.model.enums.ChannelType.valueOf(ct.name()));
        }
        for (org.notification.model.enums.NotificationPriority np : org.notification.model.enums.NotificationPriority.values()) {
            assertNotNull(org.notification.model.enums.NotificationPriority.valueOf(np.name()));
        }
        for (org.notification.model.enums.RoleType rt : org.notification.model.enums.RoleType.values()) {
            assertNotNull(org.notification.model.enums.RoleType.valueOf(rt.name()));
        }
    }
}

