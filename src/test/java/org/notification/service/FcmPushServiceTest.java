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

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        for (com.google.firebase.FirebaseApp app : new java.util.ArrayList<>(com.google.firebase.FirebaseApp.getApps())) {
            app.delete();
        }
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
        assertDoesNotThrow(() ->
                fcmPushService.sendPushNotification("u1", "Title", "Body", null, null)
        );
    }

    @Test
    void testSendPushNotification_WithTokens_SuccessAndErrors() {
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseOptions.builder()
                .setCredentials(mock(com.google.auth.oauth2.GoogleCredentials.class))
                .setProjectId("dummy-project")
                .build();
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(options);
        }

        org.notification.model.DeviceToken dt1 = new org.notification.model.DeviceToken();
        dt1.setToken("token1");
        dt1.setUserId("u1");

        org.notification.model.DeviceToken dt2 = new org.notification.model.DeviceToken();
        dt2.setToken("token2");
        dt2.setUserId("u1");

        when(repository.findByUserId("u1")).thenReturn(java.util.List.of(dt1, dt2));

        FcmPushService mockFcmService = new FcmPushService(repository) {
            @Override
            protected String sendFirebaseMessage(com.google.firebase.messaging.Message message) throws com.google.firebase.messaging.FirebaseMessagingException {
                if (message.toString().contains("token2")) {
                    throw new RuntimeException("Generic FCM Error");
                }
                return "projects/dummy/messages/123";
            }
        };

        assertDoesNotThrow(() -> mockFcmService.sendPushNotification("u1", "Title", "Body", "GENERAL", "ref1"));
    }

    @Test
    void testSendPushNotification_EmptyDeviceTokensList() {
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseOptions.builder()
                .setCredentials(mock(com.google.auth.oauth2.GoogleCredentials.class))
                .setProjectId("dummy-project")
                .build();
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(options);
        }

        when(repository.findByUserId("u_empty")).thenReturn(java.util.Collections.emptyList());

        assertDoesNotThrow(() -> fcmPushService.sendPushNotification("u_empty", "Title", "Body", null, null));
    }
}

