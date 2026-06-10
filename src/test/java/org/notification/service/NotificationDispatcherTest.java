package org.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.config.ProviderConfigValidator;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationChannel;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private SmsSenderService smsSenderService;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Stubs the static provider flags: email=true, sms=true. */
    private static void stubProvidersEnabled(MockedStatic<ProviderConfigValidator> mocked) {
        mocked.when(ProviderConfigValidator::isEmailEnabled).thenReturn(true);
        mocked.when(ProviderConfigValidator::isSmsEnabled).thenReturn(true);
    }

    // ── email dispatch ─────────────────────────────────────────────────────────

    @Test
    void testDispatchEmail() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.EMAIL);
            n.setRecipientEmail("test@example.com");
            n.setTitle("Title");
            n.setMessage("Message");

            dispatcher.dispatch(n);

            verify(emailSenderService, times(1)).sendEmail("test@example.com", "Title", "Message");
            verifyNoInteractions(smsSenderService);
        }
    }

    @Test
    void testDispatchEmail_ProviderDisabled() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            mocked.when(ProviderConfigValidator::isEmailEnabled).thenReturn(false);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.EMAIL);

            assertThrows(org.notification.exception.ProviderDisabledException.class, () -> dispatcher.dispatch(n));
            verifyNoInteractions(emailSenderService);
        }
    }

    @Test
    void testDispatchEmail_MissingEmail() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.EMAIL);
            n.setRecipientEmail("");

            assertThrows(org.notification.exception.PermanentFailureException.class, () -> dispatcher.dispatch(n));
            verifyNoInteractions(emailSenderService);
        }
    }

    @Test
    void testDispatchEmail_InvalidEmailException() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.EMAIL);
            n.setRecipientEmail("bad-email");
            n.setTitle("Title");
            n.setMessage("Msg");

            doThrow(new RuntimeException("invalid address")).when(emailSenderService).sendEmail(any(), any(), any());

            assertThrows(org.notification.exception.PermanentFailureException.class, () -> dispatcher.dispatch(n));
        }
    }

    @Test
    void testDispatchEmail_GenericException() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.EMAIL);
            n.setRecipientEmail("good@example.com");
            n.setTitle("Title");
            n.setMessage("Msg");

            doThrow(new RuntimeException("SMTP Server Down")).when(emailSenderService).sendEmail(any(), any(), any());

            assertThrows(RuntimeException.class, () -> dispatcher.dispatch(n));
        }
    }

    // ── SMS dispatch ───────────────────────────────────────────────────────────

    @Test
    void testDispatchSms() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.SMS);
            n.setRecipientPhone("+919000000000");
            n.setTitle("Alert");
            n.setMessage("Message");

            dispatcher.dispatch(n);

            verify(smsSenderService, times(1)).sendSms("+919000000000", "Alert: Message");
            verifyNoInteractions(emailSenderService);
        }
    }

    @Test
    void testDispatchSms_ProviderDisabled() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            mocked.when(ProviderConfigValidator::isSmsEnabled).thenReturn(false);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.SMS);

            assertThrows(org.notification.exception.ProviderDisabledException.class, () -> dispatcher.dispatch(n));
            verifyNoInteractions(smsSenderService);
        }
    }

    @Test
    void testDispatchSms_MissingPhone() {
        try (MockedStatic<ProviderConfigValidator> mocked = mockStatic(ProviderConfigValidator.class)) {
            stubProvidersEnabled(mocked);

            Notification n = new Notification();
            n.setChannel(NotificationChannel.SMS);
            n.setRecipientPhone(null);

            assertThrows(org.notification.exception.PermanentFailureException.class, () -> dispatcher.dispatch(n));
            verifyNoInteractions(smsSenderService);
        }
    }

    // ── in-app dispatch ────────────────────────────────────────────────────────

    @Test
    void testDispatchInApp() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.IN_APP);

        dispatcher.dispatch(n);

        verifyNoInteractions(emailSenderService);
        verifyNoInteractions(smsSenderService);
    }
}
