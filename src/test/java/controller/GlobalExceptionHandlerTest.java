package controller;

import org.junit.jupiter.api.Test;
import org.notification.controller.GlobalExceptionHandler;
import org.notification.controller.NotificationController;
import org.notification.model.Notification;
import org.notification.security.JwtUtil;
import org.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@ContextConfiguration(classes = {NotificationController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @MockBean NotificationService service;
    @MockBean JwtUtil jwtUtil;

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        // Send empty notification body to trigger @Valid failure
        Notification n = new Notification(); // missing required fields

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
