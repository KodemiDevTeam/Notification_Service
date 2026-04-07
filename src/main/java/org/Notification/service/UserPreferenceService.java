package org.Notification.service;

import org.Notification.model.UserPreference;
import org.Notification.repository.UserPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceService {

    @Autowired
    private UserPreferenceRepository repo;

    // ✅ SAVE OR UPDATE PREFERENCE
    public UserPreference save(UserPreference pref) {

        return repo.save(pref);
    }

    // ✅ GET BY USER
    public UserPreference getByUserId(String userId) {

        return repo.findByUserId(userId);
    }
}