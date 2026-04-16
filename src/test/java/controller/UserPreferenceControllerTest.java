package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.notification.controller.UserPreferenceController;
import org.notification.model.UserPreference;
import org.notification.security.JwtUtil;
import org.notification.service.UserPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserPreferenceController.class)
@ContextConfiguration(classes = {UserPreferenceController.class})
class UserPreferenceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserPreferenceService service;
    @MockBean JwtUtil jwtUtil;

    private static final String AUTH = "Bearer test-token";

    @Test
    void POST_shouldSavePreference() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        UserPreference pref = buildPref("user1");
        when(service.save(any())).thenReturn(pref);

        mockMvc.perform(post("/api/preferences")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pref)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"));

        verify(service, times(1)).save(any());
    }

    @Test
    void GET_shouldReturnPreference() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        UserPreference pref = buildPref("user1");
        when(service.getByUserId("user1")).thenReturn(pref);

        mockMvc.perform(get("/api/preferences").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"));
    }

    private UserPreference buildPref(String userId) {
        UserPreference pref = new UserPreference();
        pref.setUserId(userId);
        pref.setStudentFeedback(true);
        pref.setLiveClassReminder(true);
        return pref;
    }
}
