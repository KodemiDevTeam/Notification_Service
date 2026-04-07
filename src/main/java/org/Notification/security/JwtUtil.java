package org.Notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final String SECRET_KEY =
            "k8Jd9wQ2x+4aB3d1FJtL8vZ5X0yQ1V7n2gHqM4sP1tE=";

    private final long EXPIRATION_TIME =
            1000 * 60 * 60 * 10; // 10 hours

    // =========================
    // ✅ GENERATE TOKEN
    // =========================

    public String generateToken(
            String userId,
            String email,
            String role) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()

                // 🔥 IMPORTANT FIX
                .setSubject(userId)   // REAL USER ID

                .setClaims(claims)

                .setIssuedAt(new Date())

                .setExpiration(
                        new Date(System.currentTimeMillis()
                                + EXPIRATION_TIME)
                )

                .signWith(
                        SignatureAlgorithm.HS256,
                        SECRET_KEY.getBytes()
                )

                .compact();
    }

    // =========================
    // ✅ EXTRACT CLAIMS
    // =========================

    public Claims extractAllClaims(String token) {

        return Jwts.parser()

                .setSigningKey(SECRET_KEY.getBytes())

                .parseClaimsJws(token)

                .getBody();
    }

    // =========================
    // ✅ GET USER ID
    // =========================

    public String getUserId(String token) {

        return extractAllClaims(token)
                .get("userId", String.class);
    }

    public String getEmail(String token) {

        return extractAllClaims(token)
                .get("email", String.class);
    }

    public String getRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }
}