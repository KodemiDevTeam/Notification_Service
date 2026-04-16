package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.exception.InAppNotificationException;
import org.notification.model.InAppNotification;
import org.notification.model.enums.NotificationType;
import org.notification.repository.InAppRepository;
import org.notification.service.InAppService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppServiceTest {

    @Mock InAppRepository inAppRepo;
    @InjectMocks InAppService inAppService;

    @Test
    void createInApp_shouldSaveSuccessfully() {
        doNothing().when(inAppRepo).save(any(InAppNotification.class));
        assertDoesNotThrow(() -> inAppService.createInApp("user1", "Title", "Desc", NotificationType.COURSE_ALERT, "url"));
        verify(inAppRepo, times(1)).save(any(InAppNotification.class));
    }

    @Test
    void createInApp_shouldThrowWhenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                inAppService.createInApp(null, "Title", "Desc", NotificationType.COURSE_ALERT, null));
    }

    @Test
    void createInApp_shouldThrowWhenUserIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                inAppService.createInApp("", "Title", "Desc", NotificationType.COURSE_ALERT, null));
    }

    @Test
    void createInApp_shouldThrowWhenTitleIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                inAppService.createInApp("user1", null, "Desc", NotificationType.COURSE_ALERT, null));
    }

    @Test
    void createInApp_shouldThrowWhenDescIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                inAppService.createInApp("user1", "Title", null, NotificationType.COURSE_ALERT, null));
    }

    @Test
    void createInApp_shouldThrowWhenTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                inAppService.createInApp("user1", "Title", "Desc", null, null));
    }

    @Test
    void createInApp_shouldThrowInAppExceptionOnRepoFailure() {
        doThrow(new RuntimeException("DB error")).when(inAppRepo).save(any());
        assertThrows(InAppNotificationException.class, () ->
                inAppService.createInApp("user1", "Title", "Desc", NotificationType.COURSE_ALERT, null));
    }
}
