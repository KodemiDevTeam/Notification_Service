package org.notification.controller;

import org.notification.model.Notification;
import org.notification.service.NotificationService;
import org.notification.security.JwtUtil;
import org.notification.exception.InvalidAuthorizationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String AUTH_PREFIX = "Bearer ";

    private final NotificationService service;
    private final JwtUtil jwtUtil;

    // Constructor Injection (Fixes @Autowired warning)
    public NotificationController(
            NotificationService service,
            JwtUtil jwtUtil) {

        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    // Utility method to extract token (Removes duplication)
    private String extractToken(String authHeader) {

        if (authHeader == null
                || !authHeader.startsWith(AUTH_PREFIX)) {

            throw new InvalidAuthorizationException(
                    "Invalid Authorization Header"
            );
        }

        return authHeader.substring(AUTH_PREFIX.length());
    }

    // CREATE NOTIFICATION
    @PostMapping
    public ResponseEntity<Notification> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Notification n) {

        String token = extractToken(authHeader);

        String userId = jwtUtil.getUserId(token);
        String email = jwtUtil.getEmail(token);

        Notification saved =
                service.create(n, userId, email);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    // GET USER NOTIFICATIONS
    @GetMapping
    public ResponseEntity<List<Notification>> getByUser(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);

        String userId = jwtUtil.getUserId(token);

        List<Notification> notifications =
                service.getByUser(userId);

        return ResponseEntity.ok(notifications);
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(
            @PathVariable String id) {

        Notification notification =
                service.getById(id);

        return ResponseEntity.ok(notification);
    }

    // UPDATE NOTIFICATION
    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
            @PathVariable String id,
            @Valid @RequestBody Notification updatedNotification) {

        Notification updated =
                service.update(id, updatedNotification);

        return ResponseEntity.ok(updated);
    }

    // DELETE NOTIFICATION
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable String id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Notification deleted successfully");
    }
}