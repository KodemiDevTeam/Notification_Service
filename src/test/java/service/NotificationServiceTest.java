package service;

import org.notification.exception.NotificationNotFoundException;
import org.notification.exception.NotificationValidationException;
import org.notification.model.Notification;
import org.notification.model.UserPreference;
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
import java.util.Optional;

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

    // ==============================
    // CREATE - validation
    // ==============================

    @Test
    void create_shouldThrowWhenUserIdIsNull() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        assertThrows(NotificationValidationException.class, () -> service.create(n, null, "a@b.com"));
    }

    @Test
    void create_shouldThrowWhenTypeIsNull() {
        Notification n = buildNotification("EMAIL", null);
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldThrowWhenChannelIsNull() {
        Notification n = buildNotification(null, NotificationType.COURSE_ALERT);
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    // ==============================
    // CREATE - EMAIL channel
    // ==============================

    @Test
    void create_shouldSendEmailAndSetSent() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "a@b.com");

        assertEquals("SENT", result.getStatus());
        verify(emailService, times(1)).sendEmail(any(), any(), any());
    }

    @Test
    void create_shouldSetStatusFailedWhenEmailThrows() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);
        doThrow(new RuntimeException("fail")).when(emailService).sendEmail(any(), any(), any());

        Notification result = service.create(n, "user1", "a@b.com");

        assertEquals("FAILED", result.getStatus());
    }

    @Test
    void create_shouldThrowWhenEmailMissingForEmailChannel() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        n.setEmail(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", null);
        assertEquals("FAILED", result.getStatus());
    }

    // ==============================
    // CREATE - SMS channel
    // ==============================

    @Test
    void create_shouldSendSmsAndSetSent() {
        Notification n = buildNotification("SMS", NotificationType.COURSE_ALERT);
        n.setPhoneNumber("+91999");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "a@b.com");

        assertEquals("SENT", result.getStatus());
        verify(smsService, times(1)).sendSms(any(), any());
    }

    @Test
    void create_shouldSetFailedWhenPhoneMissingForSms() {
        Notification n = buildNotification("SMS", NotificationType.COURSE_ALERT);
        n.setPhoneNumber(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "a@b.com");
        assertEquals("FAILED", result.getStatus());
    }

    // ==============================
    // CREATE - IN_APP channel
    // ==============================

    @Test
    void create_shouldSendInAppAndSetSent() {
        Notification n = buildNotification("IN_APP", NotificationType.COURSE_ALERT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "a@b.com");

        assertEquals("SENT", result.getStatus());
        verify(inAppService, times(1)).createInApp(any(), any(), any(), any(), any());
    }

    // ==============================
    // CREATE - isRead/isScheduled null defaults
    // ==============================

    @Test
    void create_shouldSetDefaultsWhenIsReadAndIsScheduledAreNull() {
        Notification n = buildNotification("IN_APP", NotificationType.COURSE_ALERT);
        n.setIsRead(null);
        n.setIsScheduled(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(null);

        Notification result = service.create(n, "user1", "a@b.com");

        assertFalse(result.getIsRead());
        assertFalse(result.getIsScheduled());
    }

    // ==============================
    // CREATE - user preferences
    // ==============================

    @Test
    void create_shouldBlockWhenFeedbackAlertDisabled() {
        Notification n = buildNotification("EMAIL", NotificationType.FEEDBACK_ALERT);
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(false, true, true, true, true));
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldBlockWhenSessionReminderDisabled() {
        Notification n = buildNotification("EMAIL", NotificationType.SESSION_REMINDER);
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, false, true, true, true));
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldBlockWhenPayoutUpdateDisabled() {
        Notification n = buildNotification("EMAIL", NotificationType.PAYOUT_UPDATE);
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, true, false, true, true));
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldBlockWhenStreakAlertDisabled() {
        Notification n = buildNotification("EMAIL", NotificationType.STREAK_ALERT);
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, true, true, false, true));
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldBlockWhenNewEnrollmentDisabled() {
        Notification n = buildNotification("EMAIL", NotificationType.NEW_ENROLLMENT);
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, true, true, true, false));
        assertThrows(NotificationValidationException.class, () -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldAllowWhenPreferencesEnabled() {
        Notification n = buildNotification("IN_APP", NotificationType.FEEDBACK_ALERT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, true, true, true, true));

        assertDoesNotThrow(() -> service.create(n, "user1", "a@b.com"));
    }

    @Test
    void create_shouldAllowDefaultTypeWithPreferences() {
        Notification n = buildNotification("IN_APP", NotificationType.COURSE_ALERT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prefRepo.getByUserId("user1")).thenReturn(prefWith(true, true, true, true, true));

        assertDoesNotThrow(() -> service.create(n, "user1", "a@b.com"));
    }

    // ==============================
    // GET
    // ==============================

    @Test
    void getByUser_shouldReturnList() {
        when(repo.findByUserId("user1")).thenReturn(List.of(buildNotification("EMAIL", NotificationType.COURSE_ALERT)));
        assertEquals(1, service.getByUser("user1").size());
    }

    @Test
    void getByUser_shouldReturnEmptyList() {
        when(repo.findByUserId("unknown")).thenReturn(List.of());
        assertTrue(service.getByUser("unknown").isEmpty());
    }

    @Test
    void getById_shouldReturnNotification() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        n.setNotificationId("id-1");
        when(repo.findById("id-1")).thenReturn(Optional.of(n));
        assertEquals("id-1", service.getById("id-1").getNotificationId());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(repo.findById("none")).thenReturn(Optional.empty());
        assertThrows(NotificationNotFoundException.class, () -> service.getById("none"));
    }

    // ==============================
    // UPDATE
    // ==============================

    @Test
    void update_shouldUpdateAllFields() {
        Notification existing = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        existing.setNotificationId("id-1");
        when(repo.findById("id-1")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification updated = new Notification();
        updated.setTitle("New Title");
        updated.setDescription("New Desc");
        updated.setStatus("SENT");
        updated.setChannel("SMS");
        updated.setPhoneNumber("+91999");
        updated.setDeviceToken("token");

        Notification result = service.update("id-1", updated);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
        assertEquals("SENT", result.getStatus());
        assertEquals("SMS", result.getChannel());
        assertEquals("+91999", result.getPhoneNumber());
        assertEquals("token", result.getDeviceToken());
    }

    @Test
    void update_shouldNotOverwriteNullFields() {
        Notification existing = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        existing.setNotificationId("id-1");
        existing.setTitle("Original");
        when(repo.findById("id-1")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification updated = new Notification();
        // all fields null

        Notification result = service.update("id-1", updated);
        assertEquals("Original", result.getTitle());
    }

    // ==============================
    // DELETE
    // ==============================

    @Test
    void delete_shouldCallRepoDelete() {
        Notification n = buildNotification("EMAIL", NotificationType.COURSE_ALERT);
        n.setNotificationId("id-1");
        when(repo.findById("id-1")).thenReturn(Optional.of(n));
        doNothing().when(repo).delete(n);

        service.delete("id-1");
        verify(repo, times(1)).delete(n);
    }

    // ==============================
    // HELPERS
    // ==============================

    private Notification buildNotification(String channel, NotificationType type) {
        Notification n = new Notification();
        n.setTitle("Test");
        n.setDescription("Desc");
        n.setChannel(channel);
        n.setType(type);
        n.setEmail("a@b.com");
        return n;
    }

    private UserPreference prefWith(boolean feedback, boolean liveClass,
                                     boolean payout, boolean streak, boolean enrollment) {
        UserPreference pref = new UserPreference();
        pref.setStudentFeedback(feedback);
        pref.setLiveClassReminder(liveClass);
        pref.setPayoutUpdate(payout);
        pref.setStreakUpdate(streak);
        pref.setNewEnrollment(enrollment);
        return pref;
    }
}
