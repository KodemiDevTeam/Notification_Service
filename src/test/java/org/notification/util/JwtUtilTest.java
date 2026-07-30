package org.notification.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void extractUserId_withUserIdField_returnsUserId() {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"userId\":\"u123\",\"role\":\"LEARNER\"}".getBytes());
        String token = "Bearer " + header + "." + payload + ".signature";

        assertEquals("u123", jwtUtil.extractUserId(token));
        assertEquals("LEARNER", jwtUtil.extractRole(token));
    }

    @Test
    void extractUserId_withSubField_returnsSub() {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"u999\"}".getBytes());
        String token = header + "." + payload + ".signature";

        assertEquals("u999", jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_invalidTokens_returnsNull() {
        assertNull(jwtUtil.extractUserId(null));
        assertNull(jwtUtil.extractUserId(""));
        assertNull(jwtUtil.extractUserId("invalid.token"));
        assertNull(jwtUtil.extractUserId("invalid.token.structure.extra"));
        assertNull(jwtUtil.extractUserId("head.invalid_json_payload.sig"));
    }

    @Test
    void extractRole_invalidTokens_returnsNull() {
        assertNull(jwtUtil.extractRole(null));
        assertNull(jwtUtil.extractRole(""));
        assertNull(jwtUtil.extractRole("bad.token"));

        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"other\":\"field\"}".getBytes());
        String token = header + "." + payload + ".signature";
        assertNull(jwtUtil.extractUserId(token));
        assertNull(jwtUtil.extractRole(token));

        String invalidJsonPayload = Base64.getUrlEncoder().encodeToString("not_a_json_object".getBytes());
        String token2 = header + "." + invalidJsonPayload + ".signature";
        assertNull(jwtUtil.extractUserId(token2));
    }
}

