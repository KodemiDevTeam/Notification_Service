package org.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseConnectionManager {

    // Map of userId -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    protected SseEmitter createEmitter(long timeoutMs) {
        return new SseEmitter(timeoutMs);
    }

    public SseEmitter subscribe(String userId) {
        // Create an emitter with 1 hour timeout (3600000 ms)
        SseEmitter emitter = createEmitter(3600000L);
        
        emitters.put(userId, emitter);
        log.info("User {} connected to SSE notification stream.", userId);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE stream completed for user {}.", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE stream timed out for user {}.", userId);
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.info("SSE stream error for user {}: {}", userId, e.getMessage());
        });

        // Send an initial heartbeat/ping event to prevent connection timeout immediately
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("heartbeat"));
        } catch (IOException e) {
            log.warn("Failed to send initial heartbeat to user {}", userId);
            emitter.complete();
            emitters.remove(userId);
        }

        return emitter;
    }

    public void sendNotification(String userId, Object notificationPayload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationPayload));
                log.info("Real-time notification pushed via SSE to user {}", userId);
            } catch (IOException e) {
                log.warn("Failed to send real-time notification to user {}, removing connection", userId, e);
                emitter.complete();
                emitters.remove(userId);
            }
        }
    }
}
