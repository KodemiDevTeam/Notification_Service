package org.notification.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FirebaseConfigTest {

    @Test
    void testInitialize_GracefulExecution() {
        FirebaseConfig config = new FirebaseConfig();
        assertDoesNotThrow(config::initialize);
        // Second call tests when FirebaseApp.getApps().isEmpty() is false
        assertDoesNotThrow(config::initialize);
    }

    @Test
    void testFirebaseConfigPrivateMethods() {
        FirebaseConfig config = new FirebaseConfig();
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "buildFirebaseOptions"));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "resolveServiceAccountStream"));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "tryLoadFromPath"));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "tryLoadFromClasspath"));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "tryGetDefaultCredentialsOptions"));
    }
}

