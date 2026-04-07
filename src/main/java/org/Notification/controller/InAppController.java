package org.Notification.controller;

import lombok.extern.slf4j.Slf4j;
import org.Notification.model.InAppNotification;
import org.Notification.repository.InAppRepository;
import org.Notification.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inapps")
public class InAppController {

    @Autowired
    private InAppRepository repo;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ Get all notifications
    @GetMapping
    public List<InAppNotification> getNotifications(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);

        List<InAppNotification> list =
                new ArrayList<>(repo.getByUserId(userId));

        // sort latest first
        list.sort((a, b) ->
                Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        return list;
    }

    // ✅ Mark as read (FIXED)
    @PutMapping("/read/{id}")
    public String markAsRead(@PathVariable String id) {

        InAppNotification notif =
                repo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Notification not found"));

        // ✅ Correct logic
        notif.setIsRead(true);

        repo.save(notif);

        return "Marked as read";
    }

    // ✅ Get unread
    @GetMapping("/unread")
    public List<InAppNotification> getUnread(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);

        return repo.getUnread(userId);
    }

    // ✅ Get unread count
    @GetMapping("/unread/count")
    public int getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);

        return repo.getUnread(userId).size();
    }

    // ✅ Delete
    @DeleteMapping("/{id}")
    public String deleteNotification(@PathVariable String id) {

        repo.deleteById(id);

        return "Notification deleted";
    }

    // ✅ Get one
    @GetMapping("/one/{id}")
    public InAppNotification getOne(@PathVariable String id) {

        return repo.findById(id)
                .orElse(null);
    }
}