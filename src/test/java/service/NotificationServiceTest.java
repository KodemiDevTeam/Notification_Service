package service;

import org.notification.model.Notification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.NotificationRepository;
import org.notification.repository.UserPreferenceRepository;
import org.notification.service.NotificationService;
import org.notification.service.EmailService;
import org.notification.service.SmsService;
import org.notification.service.InAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;
    @Mock UserPreferenceRepository prefRepo;
    @Mock EmailService emailService;
    @Mock SmsService smsService;
    @Mock InAppService inAppService;
    @InjectMocks NotificationService service;

    @Test
    void create_shouldSetAutoFieldsAndSave() {
        Notification n = buildNotification("EMAIL");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "user@example.com");

        assertNotNull(result.getNotificationId());
        assertEquals("user1", result.getUserId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getCreatedAt());
        verify(repo, atLeastOnce()).save(any());
    }

    @Test
    void create_shouldCallEmailServiceForEmailChannel() {
        Notification n = buildNotification("EMAIL");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        service.create(n, "user1", "user@example.com");

        verify(emailService, times(1)).sendEmail(any(), any(), any());
    }

    @Test
    void create_shouldCallSmsServiceForSmsChannel() {
        Notification n = buildNotification("SMS");
        n.setPhoneNumber("1234567890");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        service.create(n, "user1", "user@example.com");

        verify(smsService, times(1)).sendSms(any(), any());
    }

    @Test
    void create_shouldCallInAppServiceForInAppChannel() {
        Notification n = buildNotification("IN_APP");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        service.create(n, "user1", "user@example.com");

        verify(inAppService, times(1)).createInApp(any(), any(), any(), any(), any());
    }

    @Test
    void getByUser_shouldReturnList() {
        when(repo.findByUserId("user1")).thenReturn(List.of(buildNotification("EMAIL")));

        List<Notification> result = service.getByUser("user1");

        assertEquals(1, result.size());
        verify(repo, times(1)).findByUserId("user1");
    }

    @Test
    void getByUser_shouldReturnEmptyList() {
        when(repo.findByUserId("unknown")).thenReturn(List.of());
        assertTrue(service.getByUser("unknown").isEmpty());
    }

    @Test
    void getById_shouldReturnNotification() {
        Notification n = buildNotification("EMAIL");
        n.setNotificationId("id-1");
        when(repo.findById("id-1")).thenReturn(java.util.Optional.of(n));

        Notification result = service.getById("id-1");
        assertEquals("id-1", result.getNotificationId());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(repo.findById("none")).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getById("none"));
    }

    @Test
    void delete_shouldCallRepoDelete() {
        Notification n = buildNotification("EMAIL");
        n.setNotificationId("id-1");
        when(repo.findById("id-1")).thenReturn(java.util.Optional.of(n));
        doNothing().when(repo).delete(n);

        service.delete("id-1");

        verify(repo, times(1)).delete(n);
    }

    private Notification buildNotification(String channel) {
        Notification n = new Notification();
        n.setTitle("Test Title");
        n.setDescription("Test Description");
        n.setChannel(channel);
        n.setType(NotificationType.COURSE_ALERT);
        return n;
    }
}
