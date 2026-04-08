package org.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // =========================
    // CONSTANTS
    // =========================

    private static final String secretKeyString =
            "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=";

    private static final long expirationTime =
            1000L * 60 * 60 * 10; // 10 hours

    private static final SecretKey secretKey =
            Keys.hmacShaKeyFor(secretKeyString.getBytes());

    // =========================
    // GENERATE TOKEN
    // =========================

    public String generateToken(
            String userId,
            String email,
            String role) {

        Map<String, Object> claims =
                new HashMap<>();

        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()

                .setSubject(userId)

                .setClaims(claims)

                .setIssuedAt(new Date())

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expirationTime
                        )
                )

                .signWith(secretKey)

                .compact();
    }

    // =========================
    // EXTRACT CLAIMS
    // =========================

    public Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()

                .setSigningKey(secretKey)

                .build()

                .parseClaimsJws(token)

                .getBody();
    }

    // =========================
    // GET USER ID
    // =========================

    public String getUserId(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    // =========================
    // GET EMAIL
    // =========================

    public String getEmail(String token) {

        return extractAllClaims(token)
                .get("email", String.class);
    }

    // =========================
    // GET ROLE
    // =========================

    public String getRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    // =========================
    // TOKEN VALIDATION
    // (Future Sonar-safe)
    // =========================

    public boolean isTokenExpired(String token) {

        Date expiration =
                extractAllClaims(token)
                        .getExpiration();

        return expiration.before(new Date());
    }

    public boolean validateToken(String token) {

        return !isTokenExpired(token);
    }
}