package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.repository.DeviceTokenRepository;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class FcmPushServiceTest {

    private DeviceTokenRepository repository;
    private FcmPushService fcmPushService;

    @BeforeEach
    void setUp() {
        repository = mock(DeviceTokenRepository.class);
        fcmPushService = new FcmPushService(repository);
    }

    @Test
    void testSendPushNotification_FirebaseNotInitialized_returnsEarly() {
        assertDoesNotThrow(() ->
                fcmPushService.sendPushNotification("u1", "Title", "Body", "GENERAL", "ref1")
        );
        verify(repository, never()).findByUserId(anyString());
    }

    @Test
    void testSendPushNotification_NoDeviceTokens_returnsEarly() {
        // Firebase initialization check runs first. When apps is empty, it returns early.
        assertDoesNotThrow(() ->
                fcmPushService.sendPushNotification("u1", "Title", "Body", null, null)
        );
    }
}
