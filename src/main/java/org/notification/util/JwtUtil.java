package org.notification.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Slf4j
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
        } catch (IllegalArgumentException | IOException e) {
            log.debug("Failed to extract userId from token: {}", e.getMessage());
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
        } catch (IllegalArgumentException | IOException e) {
            log.debug("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode getPayload(String token) throws IOException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String[] chunks = jwt.split("\\.");
        if (chunks.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
        return objectMapper.readTree(payloadJson);
    }
}
