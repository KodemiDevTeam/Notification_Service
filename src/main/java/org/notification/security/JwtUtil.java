package org.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // =========================
    // CONSTANTS
    // =========================

    @SuppressWarnings("java:S6418")
    private static final String SECRET_KEY_STRING =
            "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=";

    private static final long EXPIRATION_TIME =
            1000L * 60 * 60 * 10; // 10 hours

    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));

    // =========================
    // GENERATE TOKEN
    // =========================

    public String generateToken(
            String userId,
            String email,
            String role) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + EXPIRATION_TIME)
                )
                .signWith(SECRET_KEY)
                .compact();
    }

    // =========================
    // EXTRACT CLAIMS
    // =========================

    public Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================
    // GET USER ID
    // =========================

    public String getUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    // =========================
    // GET EMAIL
    // =========================

    public String getEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    // =========================
    // GET ROLE
    // =========================

    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // =========================
    // TOKEN VALIDATION
    // =========================

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        return !isTokenExpired(token);
    }
}
