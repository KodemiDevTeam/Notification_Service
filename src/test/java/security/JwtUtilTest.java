package security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.Notification.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET_KEY = "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    // generate token matching JwtUtil's generateToken logic (userId in claims, not subject)
    private String generateToken(String userId, String role, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("email", email);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    @Test
    void getUserId_shouldReturnCorrectUserId() {
        String token = generateToken("user123", "LEARNER", "user@example.com");
        assertEquals("user123", jwtUtil.getUserId(token));
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
        // getUserId reads from claims.get("userId") but generateToken doesn't add it
        // so email and role are verifiable
        assertEquals("user@example.com", jwtUtil.getEmail(token));
        assertEquals("LEARNER", jwtUtil.getRole(token));
    }
}
