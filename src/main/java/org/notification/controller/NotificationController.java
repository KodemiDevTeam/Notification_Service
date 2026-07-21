package org.notification.controller;

import jakarta.validation.Valid;
import org.notification.dto.request.BroadcastNotificationRequest;
import org.notification.dto.request.NotificationRequest;
import org.notification.dto.request.ScheduledNotificationRequest;
import org.notification.dto.response.BroadcastNotificationResponse;
import org.notification.dto.response.NotificationResponse;
import org.notification.service.NotificationService;
import org.notification.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.notification.service.SseConnectionManager;
import org.notification.repository.DeviceTokenRepository;
import org.notification.model.DeviceToken;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;
    private final JwtUtil jwtUtil;
    private final SseConnectionManager sseConnectionManager;
    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationController(NotificationService service, JwtUtil jwtUtil,
                                  SseConnectionManager sseConnectionManager,
                                  DeviceTokenRepository deviceTokenRepository) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.sseConnectionManager = sseConnectionManager;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Value("${internal.service.key:default-secret}")
    private String internalServiceKey;

    @PostMapping("/internal/broadcast")
    public ResponseEntity<BroadcastNotificationResponse> broadcastInternalNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @Valid @RequestBody BroadcastNotificationRequest request) {
        if (!internalServiceKey.equals(serviceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BroadcastNotificationResponse response = service.broadcast(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastNotificationResponse> broadcastNotification(@Valid @RequestBody BroadcastNotificationRequest request) {
        BroadcastNotificationResponse response = service.broadcast(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendNotification(@Valid @RequestBody NotificationRequest request) {
        service.sendImmediate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Notification queued for immediate sending."));
    }

    @PostMapping("/internal/send")
    public ResponseEntity<Map<String, String>> sendInternalNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @Valid @RequestBody NotificationRequest request) {
        if (!internalServiceKey.equals(serviceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.sendInternal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Internal multi-channel notification queued."));
    }

    @PostMapping("/in-app")
    public ResponseEntity<Map<String, String>> sendInAppNotification(@Valid @RequestBody NotificationRequest request) {
        request.setChannel(org.notification.model.enums.NotificationChannel.IN_APP);
        service.sendImmediate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "In-app notification queued."));
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmailNotification(@Valid @RequestBody NotificationRequest request) {
        request.setChannel(org.notification.model.enums.NotificationChannel.EMAIL);
        service.sendImmediate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Email notification queued."));
    }

    @PostMapping("/sms")
    public ResponseEntity<Map<String, String>> sendSmsNotification(@Valid @RequestBody NotificationRequest request) {
        request.setChannel(org.notification.model.enums.NotificationChannel.SMS);
        service.sendImmediate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "SMS notification queued."));
    }

    @PostMapping("/schedule")
    public ResponseEntity<Map<String, String>> scheduleNotification(@Valid @RequestBody ScheduledNotificationRequest request) {
        service.schedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Notification scheduled successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("count", service.getUnreadCount(userId)));
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<Map<String, String>> markMyAllAsRead(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        service.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All user notifications marked as read."));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @RequestHeader("Authorization") String token,
            @PathVariable String userId) {
        String tokenUserId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        if (!userId.equals(tokenUserId) && !isSuperOrUserAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("Authorization") String token,
            @PathVariable String userId) {
        String tokenUserId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        if (!userId.equals(tokenUserId) && !isSuperOrUserAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of("count", service.getUnreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String token, // Optional if we just trust the ID, but better to check owner
            @PathVariable String notificationId) {
        // ideally check if notification belongs to user, but let's keep it simple or delegate to service
        service.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String userId) {
        String tokenUserId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        if (!userId.equals(tokenUserId) && !isSuperOrUserAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All user notifications marked as read."));
    }

    private boolean isSuperOrUserAdmin(String role) {
        return role != null && (role.equals("SUPER_ADMIN") || role.equals("USER_ADMIN"));
    }

    @PatchMapping("/broadcast/{batchId}/cancel")
    public ResponseEntity<Map<String, String>> cancelBroadcast(@PathVariable String batchId) {
        service.cancelBatch(batchId);
        return ResponseEntity.ok(Map.of("message", "Broadcast batch cancelled successfully."));
    }

    @PostMapping("/broadcast/{batchId}/send-now")
    public ResponseEntity<Map<String, String>> sendNowBroadcast(@PathVariable String batchId) {
        service.sendNowBatch(batchId);
        return ResponseEntity.ok(Map.of("message", "Broadcast batch scheduled for immediate sending."));
    }

    @PatchMapping("/broadcast/{batchId}/reschedule")
    public ResponseEntity<Map<String, String>> rescheduleBroadcast(@PathVariable String batchId, @RequestBody Map<String, Long> request) {
        service.rescheduleBatch(batchId, request.get("scheduledAt"));
        return ResponseEntity.ok(Map.of("message", "Broadcast batch rescheduled successfully."));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable String notificationId) {
        service.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully."));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearUserNotifications(@PathVariable String userId) {
        service.clearUserNotifications(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared for user."));
    }

    // ─── Real-Time Stream (SSE) ───────────────────────────────────────────────
    @GetMapping(value = "/stream/{userId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String queryToken) {
            
        String token = authHeader != null ? authHeader : queryToken;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String tokenUserId = jwtUtil.extractUserId(token);
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(sseConnectionManager.subscribe(userId));
    }

    // ─── FCM Device Tokens (Mobile Push) ──────────────────────────────────────
    @PostMapping("/device-token/register")
    public ResponseEntity<Map<String, String>> registerDeviceToken(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody Map<String, String> request) {
            
        String token = tokenHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String deviceToken = request.get("token");
        String platform = request.get("platform");
        
        if (deviceToken == null || deviceToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Device token is required"));
        }
        
        DeviceToken existing = deviceTokenRepository.findByToken(deviceToken);
        if (existing == null) {
            existing = new DeviceToken();
            existing.setToken(deviceToken);
        }
        
        existing.setUserId(userId);
        existing.setPlatform(platform != null ? platform : "ANDROID");
        existing.setCreatedAt(System.currentTimeMillis());
        
        deviceTokenRepository.save(existing);
        return ResponseEntity.ok(Map.of("message", "Device token registered successfully."));
    }

    @DeleteMapping("/device-token")
    public ResponseEntity<Map<String, String>> deregisterDeviceToken(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody Map<String, String> request) {
            
        String token = tokenHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String deviceToken = request.get("token");
        if (deviceToken == null || deviceToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Device token is required"));
        }
        
        DeviceToken existing = deviceTokenRepository.findByToken(deviceToken);
        if (existing != null && userId.equals(existing.getUserId())) {
            deviceTokenRepository.delete(existing);
            return ResponseEntity.ok(Map.of("message", "Device token deregistered successfully."));
        }
        
        return ResponseEntity.ok(Map.of("message", "No matching device token found."));
    }
}