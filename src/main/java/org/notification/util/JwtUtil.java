package org.notification.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.notification.exception.InvalidJwtException;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class JwtUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractUserId(String token) {
        try {
            JsonNode payload = getPayload(token);
            if (payload.has("userId")) {
                return payload.get("userId").asText();
            }
            if (payload.has("sub")) {
                return payload.get("sub").asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            JsonNode payload = getPayload(token);
            if (payload.has("role")) {
                return payload.get("role").asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getPayload(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Token cannot be null or blank");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String[] chunks = token.split("\\.");
        if (chunks.length != 3) {
            throw new InvalidJwtException("Invalid JWT token");
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            return objectMapper.readTree(payloadJson);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new InvalidJwtException("Invalid JWT payload", e);
        }
    }
}
