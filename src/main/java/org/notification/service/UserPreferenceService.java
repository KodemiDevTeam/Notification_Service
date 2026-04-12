package org.notification.service;

import org.notification.model.UserPreference;
import org.notification.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository repo;

    public UserPreferenceService(UserPreferenceRepository repo) {
        this.repo = repo;
    }

    // SAVE OR UPDATE PREFERENCE
    public UserPreference save(UserPreference pref) {
        return repo.save(pref);
    }

    // GET BY USER
    public UserPreference getByUserId(String userId) {
        return repo.findByUserId(userId);
    }
}
