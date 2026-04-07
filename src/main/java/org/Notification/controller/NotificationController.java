package org.Notification.controller;

import org.Notification.model.Notification;
import org.Notification.service.NotificationService;
import org.Notification.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ CREATE NOTIFICATION
    @PostMapping
    public ResponseEntity<Notification> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Notification n) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization Header");
        }

        String token = authHeader.substring(7);

        String userId = jwtUtil.getUserId(token);
        String email = jwtUtil.getEmail(token);

        Notification saved =
                service.create(n, userId, email);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    // ✅ GET USER NOTIFICATIONS
    @GetMapping
    public ResponseEntity<List<Notification>> getByUser(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization Header");
        }

        String token = authHeader.substring(7);

        String userId = jwtUtil.getUserId(token);

        List<Notification> notifications =
                service.getByUser(userId);

        return ResponseEntity.ok(notifications);
    }

    // ✅ GET BY ID (NEW — IMPORTANT)
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(
            @PathVariable String id) {

        Notification notification =
                service.getById(id);

        return ResponseEntity.ok(notification);
    }

    // ✅ UPDATE NOTIFICATION
    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
            @PathVariable String id,
            @RequestBody Notification updatedNotification) {

        Notification updated =
                service.update(id, updatedNotification);

        return ResponseEntity.ok(updated);
    }

    // ✅ DELETE NOTIFICATION
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable String id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Notification deleted successfully");
    }
}