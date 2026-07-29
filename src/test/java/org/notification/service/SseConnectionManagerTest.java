package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseConnectionManagerTest {

    private SseConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new SseConnectionManager();
    }

    @Test
    void testSubscribe_returnsSseEmitter() {
        SseEmitter emitter = connectionManager.subscribe("user1");
        assertNotNull(emitter);
    }

    @Test
    void testSubscribe_callbacksAndHeartbeatException() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Heartbeat failed")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        SseConnectionManager customManager = new SseConnectionManager() {
            @Override
            protected SseEmitter createEmitter(long timeoutMs) {
                return mockEmitter;
            }
        };

        SseEmitter res = customManager.subscribe("u_heartbeat");
        assertEquals(mockEmitter, res);
        verify(mockEmitter).complete();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockEmitter).onCompletion(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        verify(mockEmitter).onTimeout(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Throwable>> consumerCaptor = ArgumentCaptor.forClass((Class) Consumer.class);
        verify(mockEmitter).onError(consumerCaptor.capture());
        consumerCaptor.getValue().accept(new Exception("SSE error"));
    }

    @Test
    void testSendNotification_activeUser_doesNotThrow() {
        connectionManager.subscribe("user1");
        assertDoesNotThrow(() ->
                connectionManager.sendNotification("user1", "test payload")
        );
    }

    @Test
    void testSendNotification_inactiveUser_doesNotThrow() {
        assertDoesNotThrow(() ->
                connectionManager.sendNotification("user_non_existent", "test payload")
        );
    }

    @Test
    void testSendNotification_ioException_removesEmitter() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Connection reset")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        @SuppressWarnings("unchecked")
        Map<String, SseEmitter> emitters = (Map<String, SseEmitter>) ReflectionTestUtils.getField(connectionManager, "emitters");
        assertNotNull(emitters);
        emitters.put("user_err", mockEmitter);

        assertDoesNotThrow(() -> connectionManager.sendNotification("user_err", "payload"));
        verify(mockEmitter).complete();
        assertFalse(emitters.containsKey("user_err"));
    }
}
