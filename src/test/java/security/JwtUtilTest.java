package security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.security.JwtUtil;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject the secret the same way Spring would via @Value
        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=");
        jwtUtil.init();
    }

    @Test
    void generateToken_shouldProduceValidToken() {
        String token = jwtUtil.generateToken("user1", "user@example.com", "LEARNER");
        assertNotNull(token);
        assertEquals("user1", jwtUtil.getUserId(token));
        assertEquals("user@example.com", jwtUtil.getEmail(token));
        assertEquals("LEARNER", jwtUtil.getRole(token));
    }

    @Test
    void getUserId_shouldReturnCorrectUserId() {
        String token = jwtUtil.generateToken("user123", "user@example.com", "LEARNER");
        assertEquals("user123", jwtUtil.getUserId(token));
    }

    @Test
    void getRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateToken("user123", "user@example.com", "ADMIN");
        assertEquals("ADMIN", jwtUtil.getRole(token));
    }

    @Test
    void getEmail_shouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken("user123", "trainer@example.com", "TRAINER");
        assertEquals("trainer@example.com", jwtUtil.getEmail(token));
    }

    @Test
    void extractAllClaims_shouldNotBeNull() {
        String token = jwtUtil.generateToken("user456", "learner@example.com", "LEARNER");
        assertNotNull(jwtUtil.extractAllClaims(token));
    }

    @Test
    void getUserId_shouldThrowForInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.getUserId("invalid.token.here"));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("user1", "user@example.com", "LEARNER");
        assertTrue(jwtUtil.validateToken(token));
    }
}
