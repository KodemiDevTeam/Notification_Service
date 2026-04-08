package controller;

import org.notification.controller.InAppController;
import org.notification.model.InAppNotification;
import org.notification.repository.InAppRepository;
import org.notification.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InAppController.class)
class InAppControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean InAppRepository repo;
    @MockBean JwtUtil jwtUtil;

    private static final String AUTH = "Bearer test-token";

    @Test
    void GET_shouldReturnNotifications() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        InAppNotification n1 = buildNotif("id-1", "user1", 1000L);
        InAppNotification n2 = buildNotif("id-2", "user1", 2000L);
        when(repo.getByUserId("user1")).thenReturn(List.of(n1, n2));

        mockMvc.perform(get("/api/inapps").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-2")) // sorted latest first
                .andExpect(jsonPath("$[1].id").value("id-1"));
    }

    @Test
    void GET_shouldReturnEmptyList() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        when(repo.getByUserId("user1")).thenReturn(List.of());

        mockMvc.perform(get("/api/inapps").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void PUT_markAsRead_shouldReturnMarkedAsRead() throws Exception {
        InAppNotification n = buildNotif("id-1", "user1", 1000L);
        when(repo.findById("id-1")).thenReturn(Optional.of(n));
        doNothing().when(repo).save(n);

        mockMvc.perform(put("/api/inapps/read/id-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Marked as read"));
    }

    @Test
    void GET_unread_shouldReturnUnread() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        InAppNotification n = buildNotif("id-1", "user1", 1000L);
        when(repo.getUnread("user1")).thenReturn(List.of(n));

        mockMvc.perform(get("/api/inapps/unread").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-1"));
    }

    @Test
    void GET_unreadCount_shouldReturnCount() throws Exception {
        when(jwtUtil.getUserId("test-token")).thenReturn("user1");
        InAppNotification n = buildNotif("id-1", "user1", 1000L);
        when(repo.getUnread("user1")).thenReturn(List.of(n));

        mockMvc.perform(get("/api/inapps/unread/count").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void DELETE_shouldReturnMessage() throws Exception {
        doNothing().when(repo).deleteById("id-1");

        mockMvc.perform(delete("/api/inapps/id-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted"));

        verify(repo, times(1)).deleteById("id-1");
    }

    @Test
    void GET_one_shouldReturnNotification() throws Exception {
        InAppNotification n = buildNotif("id-1", "user1", 1000L);
        when(repo.findById("id-1")).thenReturn(Optional.of(n));

        mockMvc.perform(get("/api/inapps/one/id-1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void GET_one_shouldReturnNullWhenNotFound() throws Exception {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/inapps/one/missing"))
                .andExpect(status().isOk());
    }

    private InAppNotification buildNotif(String id, String userId, long createdAt) {
        InAppNotification n = new InAppNotification();
        n.setId(id);
        n.setUserId(userId);
        n.setCreatedAt(createdAt);
        n.setIsRead(false);
        return n;
    }
}
