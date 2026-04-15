package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.notification.controller.NotificationController;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;
import org.notification.security.JwtUtil;
import org.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@ContextConfiguration(classes = {NotificationController.class})
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean NotificationService service;
    @MockBean JwtUtil jwtUtil;

    private static final String AUTH = "Bearer test-token";

    @Test
    void POST_shouldCreateNotificationFromToken() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        when(jwtUtil.getEmail("test-token")).thenReturn("user@example.com");

        Notification n = buildNotification();
        n.setNotificationId("id-1");
        when(service.create(any(), eq("user1"), eq("user@example.com"))).thenReturn(n);

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(n)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").value("id-1"));

        verify(service, times(1)).create(any(), eq("user1"), eq("user@example.com"));
    }

    @Test
    void GET_shouldReturnNotificationsFromToken() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        Notification n = buildNotification();
        n.setNotificationId("id-1");
        when(service.getByUser("user1")).thenReturn(List.of(n));

        mockMvc.perform(get("/api/notifications").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value("id-1"));
    }

    @Test
    void GET_shouldReturnEmptyList() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        when(service.getByUser("user1")).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void GET_byId_shouldReturnNotification() throws Exception {
        Notification n = buildNotification();
        n.setNotificationId("id-1");
        when(service.getById("id-1")).thenReturn(n);

        mockMvc.perform(get("/api/notifications/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value("id-1"));
    }

    @Test
    void PUT_shouldUpdateNotification() throws Exception {
        Notification updated = buildNotification();
        updated.setNotificationId("id-1");
        when(service.update(eq("id-1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/notifications/id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value("id-1"));
    }

    @Test
    void DELETE_shouldReturn200() throws Exception {
        doNothing().when(service).delete("id-1");

        mockMvc.perform(delete("/api/notifications/id-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted successfully"));
    }

    private Notification buildNotification() {
        Notification n = new Notification();
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Test desc");
        n.setChannel("EMAIL");
        n.setType(NotificationType.COURSE_ALERT);
        n.setStatus("PENDING");
        return n;
    }
}
