package org.notification.controller;

import lombok.extern.slf4j.Slf4j;
import org.notification.model.InAppNotification;
import org.notification.repository.InAppRepository;
import org.notification.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inapps")
public class InAppController {

    private final InAppRepository repo;
    private final JwtUtil jwtUtil;

    public InAppController(InAppRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    // Get all notifications
    @GetMapping
    public List<InAppNotification> getNotifications(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);

        List<InAppNotification> list =
                new ArrayList<>(repo.getByUserId(userId));

        list.sort((a, b) ->
                Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        return list;
    }

    // Mark as read
    @PutMapping("/read/{id}")
    public String markAsRead(@PathVariable String id) {

        InAppNotification notif =
                repo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Notification not found"));

        notif.setIsRead(true);
        repo.save(notif);
        return "Marked as read";
    }

    // Get unread
    @GetMapping("/unread")
    public List<InAppNotification> getUnread(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);
        return repo.getUnread(userId);
    }

    // Get unread count
    @GetMapping("/unread/count")
    public int getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);
        return repo.getUnread(userId).size();
    }

    // Delete
    @DeleteMapping("/{id}")
    public String deleteNotification(@PathVariable String id) {
        repo.deleteById(id);
        return "Notification deleted";
    }

    // Get one
    @GetMapping("/one/{id}")
    public ResponseEntity<InAppNotification> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
