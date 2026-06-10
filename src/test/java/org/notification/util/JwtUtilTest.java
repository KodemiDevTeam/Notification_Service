package org.notification.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // A minimal JWT: header.payload.signature (signature is ignored by JwtUtil)
    private static String buildToken(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".sig";
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    // ── extractUserId ───────────────────────────────────────────────────────────

    @Test
    void testExtractUserId_FromUserId() {
        String token = buildToken("{\"userId\":\"u123\",\"role\":\"LEARNER\"}");
        assertEquals("u123", jwtUtil.extractUserId(token));
    }

    @Test
    void testExtractUserId_FromSub_WhenNoUserId() {
        String token = buildToken("{\"sub\":\"u456\",\"role\":\"TRAINER\"}");
        assertEquals("u456", jwtUtil.extractUserId(token));
    }

    @Test
    void testExtractUserId_NeitherField_ReturnsNull() {
        String token = buildToken("{\"role\":\"ADMIN\"}");
        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void testExtractUserId_WithBearerPrefix() {
        String token = "Bearer " + buildToken("{\"userId\":\"u789\"}");
        assertEquals("u789", jwtUtil.extractUserId(token));
    }

    // ── extractRole ─────────────────────────────────────────────────────────────

    @Test
    void testExtractRole_Success() {
        String token = buildToken("{\"userId\":\"u1\",\"role\":\"SUPER_ADMIN\"}");
        assertEquals("SUPER_ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void testExtractRole_NoRole_ReturnsNull() {
        String token = buildToken("{\"userId\":\"u1\"}");
        assertNull(jwtUtil.extractRole(token));
    }

    // ── invalid / edge cases ────────────────────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "notajwt", "only.one"})
    void testExtractUserId_InvalidToken_ReturnsNull(String token) {
        assertNull(jwtUtil.extractUserId(token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "notajwt", "only.one"})
    void testExtractRole_InvalidToken_ReturnsNull(String token) {
        assertNull(jwtUtil.extractRole(token));
    }

    @Test
    void testExtractUserId_MalformedPayloadBase64_ReturnsNull() {
        // payload is not valid base64
        assertNull(jwtUtil.extractUserId("header.!!!invalid!!!.sig"));
    }
}
