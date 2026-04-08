package security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.notification.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must match the secret in JwtUtil
    private static final String SECRET_KEY_STRING = "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    /**
     * Mirrors JwtUtil.generateToken: userId as subject, then setClaims(email+role).
     * Note: in jjwt 0.11.x, setClaims() after setSubject() replaces the claims map,
     * so subject ends up null. getUserId() returns null for tokens built this way.
     */
    private String generateToken(String userId, String role, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        return Jwts.builder()
                .setSubject(userId)
                .setClaims(claims)   // overwrites subject in jjwt 0.11.x
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SECRET_KEY)
                .compact();
    }

    @Test
    void getRole_shouldReturnCorrectRole() {
        String token = generateToken("user123", "ADMIN", "user@example.com");
        assertEquals("ADMIN", jwtUtil.getRole(token));
    }

    @Test
    void getEmail_shouldReturnCorrectEmail() {
        String token = generateToken("user123", "TRAINER", "trainer@example.com");
        assertEquals("trainer@example.com", jwtUtil.getEmail(token));
    }

    @Test
    void extractAllClaims_shouldNotBeNull() {
        String token = generateToken("user456", "LEARNER", "learner@example.com");
        assertNotNull(jwtUtil.extractAllClaims(token));
    }

    @Test
    void getUserId_shouldThrowForInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.getUserId("invalid.token.here"));
    }

    @Test
    void generateToken_shouldProduceValidToken() {
        String token = jwtUtil.generateToken("user1", "user@example.com", "LEARNER");
        assertNotNull(token);
        assertEquals("user@example.com", jwtUtil.getEmail(token));
        assertEquals("LEARNER", jwtUtil.getRole(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("user1", "user@example.com", "LEARNER");
        assertTrue(jwtUtil.validateToken(token));
    }
}
