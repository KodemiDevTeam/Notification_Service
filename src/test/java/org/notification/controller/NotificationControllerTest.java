package org.notification.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.dto.request.BroadcastNotificationRequest;
import org.notification.dto.request.NotificationRequest;
import org.notification.dto.request.ScheduledNotificationRequest;
import org.notification.dto.response.BroadcastNotificationResponse;
import org.notification.dto.response.NotificationResponse;
import org.notification.service.NotificationService;
import org.notification.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private NotificationController controller;

    private static final String SERVICE_KEY = "test-key";
    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "internalServiceKey", SERVICE_KEY);
    }

    @Test
    void testBroadcastInternalNotification_Success() {
        BroadcastNotificationResponse response = BroadcastNotificationResponse.builder().batchId("b1").build();
        when(notificationService.broadcast(any())).thenReturn(response);

        ResponseEntity<BroadcastNotificationResponse> res = controller.broadcastInternalNotification(SERVICE_KEY, new BroadcastNotificationRequest());

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals("b1", res.getBody().getBatchId());
    }

    @Test
    void testBroadcastInternalNotification_Forbidden() {
        ResponseEntity<BroadcastNotificationResponse> res = controller.broadcastInternalNotification("wrong-key", new BroadcastNotificationRequest());
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void testBroadcastNotification() {
        BroadcastNotificationResponse response = BroadcastNotificationResponse.builder().batchId("b1").build();
        when(notificationService.broadcast(any())).thenReturn(response);

        ResponseEntity<BroadcastNotificationResponse> res = controller.broadcastNotification(new BroadcastNotificationRequest());

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals("b1", res.getBody().getBatchId());
    }

    @Test
    void testHealthCheck() {
        ResponseEntity<Map<String, String>> res = controller.healthCheck();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("UP", res.getBody().get("status"));
    }

    @Test
    void testSendNotification() {
        ResponseEntity<Map<String, String>> res = controller.sendNotification(new NotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).sendImmediate(any());
    }

    @Test
    void testSendInternalNotification_Success() {
        ResponseEntity<Map<String, String>> res = controller.sendInternalNotification(SERVICE_KEY, new NotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).sendInternal(any());
    }

    @Test
    void testSendInternalNotification_Forbidden() {
        ResponseEntity<Map<String, String>> res = controller.sendInternalNotification("wrong-key", new NotificationRequest());
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void testSendInAppNotification() {
        ResponseEntity<Map<String, String>> res = controller.sendInAppNotification(new NotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).sendImmediate(any());
    }

    @Test
    void testSendEmailNotification() {
        ResponseEntity<Map<String, String>> res = controller.sendEmailNotification(new NotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).sendImmediate(any());
    }

    @Test
    void testSendSmsNotification() {
        ResponseEntity<Map<String, String>> res = controller.sendSmsNotification(new NotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).sendImmediate(any());
    }

    @Test
    void testScheduleNotification() {
        ResponseEntity<Map<String, String>> res = controller.scheduleNotification(new ScheduledNotificationRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        verify(notificationService, times(1)).schedule(any());
    }

    @Test
    void testGetMyNotifications_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");
        when(notificationService.getUserNotifications("u1")).thenReturn(List.of());

        ResponseEntity<List<NotificationResponse>> res = controller.getMyNotifications(TOKEN);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(0, res.getBody().size());
    }

    @Test
    void testGetMyNotifications_Unauthorized() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(null);

        ResponseEntity<List<NotificationResponse>> res = controller.getMyNotifications(TOKEN);

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void testGetMyUnreadCount_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");
        when(notificationService.getUnreadCount("u1")).thenReturn(5L);

        ResponseEntity<Map<String, Long>> res = controller.getMyUnreadCount(TOKEN);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(5L, res.getBody().get("count"));
    }

    @Test
    void testGetMyUnreadCount_Unauthorized() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(null);

        ResponseEntity<Map<String, Long>> res = controller.getMyUnreadCount(TOKEN);

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void testMarkMyAllAsRead_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");

        ResponseEntity<Map<String, String>> res = controller.markMyAllAsRead(TOKEN);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).markAllAsRead("u1");
    }

    @Test
    void testMarkMyAllAsRead_Unauthorized() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(null);

        ResponseEntity<Map<String, String>> res = controller.markMyAllAsRead(TOKEN);

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void testGetUserNotifications_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");
        when(notificationService.getUserNotifications("u1")).thenReturn(List.of());

        ResponseEntity<List<NotificationResponse>> res = controller.getUserNotifications(TOKEN, "u1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetUserNotifications_AdminSuccess() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin1");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("SUPER_ADMIN");
        when(notificationService.getUserNotifications("u1")).thenReturn(List.of());

        ResponseEntity<List<NotificationResponse>> res = controller.getUserNotifications(TOKEN, "u1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetUserNotifications_Forbidden() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("otheruser");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");

        ResponseEntity<List<NotificationResponse>> res = controller.getUserNotifications(TOKEN, "u1");

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void testGetUnreadCount_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");
        when(notificationService.getUnreadCount("u1")).thenReturn(3L);

        ResponseEntity<Map<String, Long>> res = controller.getUnreadCount(TOKEN, "u1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(3L, res.getBody().get("count"));
    }

    @Test
    void testGetUnreadCount_Forbidden() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("otheruser");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");

        ResponseEntity<Map<String, Long>> res = controller.getUnreadCount(TOKEN, "u1");

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void testMarkAsRead() {
        ResponseEntity<Map<String, String>> res = controller.markAsRead(TOKEN, "n1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).markAsRead("n1");
    }

    @Test
    void testMarkAllAsRead_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("u1");

        ResponseEntity<Map<String, String>> res = controller.markAllAsRead(TOKEN, "u1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).markAllAsRead("u1");
    }

    @Test
    void testMarkAllAsRead_Forbidden() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("otheruser");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");

        ResponseEntity<Map<String, String>> res = controller.markAllAsRead(TOKEN, "u1");

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void testCancelBroadcast() {
        ResponseEntity<Map<String, String>> res = controller.cancelBroadcast("b1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).cancelBatch("b1");
    }

    @Test
    void testSendNowBroadcast() {
        ResponseEntity<Map<String, String>> res = controller.sendNowBroadcast("b1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).sendNowBatch("b1");
    }

    @Test
    void testRescheduleBroadcast() {
        Map<String, Long> body = new HashMap<>();
        body.put("scheduledAt", 1700000000000L);

        ResponseEntity<Map<String, String>> res = controller.rescheduleBroadcast("b1", body);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).rescheduleBatch("b1", 1700000000000L);
    }

    @Test
    void testDeleteNotification() {
        ResponseEntity<Map<String, String>> res = controller.deleteNotification("n1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).deleteNotification("n1");
    }

    @Test
    void testClearUserNotifications() {
        ResponseEntity<Map<String, String>> res = controller.clearUserNotifications("u1");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService, times(1)).clearUserNotifications("u1");
    }
}
