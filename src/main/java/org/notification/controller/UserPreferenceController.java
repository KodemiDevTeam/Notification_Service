package org.notification.controller;

import org.notification.model.UserPreference;
import org.notification.security.JwtUtil;
import org.notification.service.UserPreferenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class UserPreferenceController {

    @Autowired
    private UserPreferenceService service;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ CREATE / UPDATE PREFERENCE
    @PostMapping
    public UserPreference savePreference(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserPreference pref) {

        String token = authHeader.substring(7);

        String userId = jwtUtil.getUserId(token);

        // attach userId automatically
        pref.setUserId(userId);

        return service.save(pref);
    }

    // ✅ GET USER PREFERENCE
    @GetMapping
    public UserPreference getPreference(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        String userId = jwtUtil.getUserId(token);

        return service.getByUserId(userId);
    }
}