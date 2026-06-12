package org.notification.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.config.ProviderConfigValidator;
import org.notification.exception.PermanentFailureException;
import org.notification.exception.ProviderDisabledException;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private SmsSenderService smsSenderService;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void enableProviders() {
        ProviderConfigValidator.setEmailEnabled(true);
        ProviderConfigValidator.setSmsEnabled(true);
    }

    @AfterEach
    void restoreProviders() {
        ProviderConfigValidator.setEmailEnabled(true);
        ProviderConfigValidator.setSmsEnabled(true);
    }

    // ── email ──────────────────────────────────────────────────────────────────

    @Test
    void testDispatchEmail_Success() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);
        n.setRecipientEmail("test@example.com");
        n.setTitle("Title");
        n.setMessage("Message");

        dispatcher.dispatch(n);

        verify(emailSenderService).sendEmail("test@example.com", "Title", "Message");
        verifyNoInteractions(smsSenderService);
    }

    @Test
    void testDispatchEmail_ProviderDisabled() {
        ProviderConfigValidator.setEmailEnabled(false);

        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);

        assertThrows(ProviderDisabledException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void testDispatchEmail_MissingEmail_ThrowsPermanentFailure() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);
        n.setRecipientEmail("");

        assertThrows(PermanentFailureException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void testDispatchEmail_NullEmail_ThrowsPermanentFailure() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);
        n.setRecipientEmail(null);

        assertThrows(PermanentFailureException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void testDispatchEmail_InvalidEmailException_WrapsAsPermanentFailure() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);
        n.setRecipientEmail("bad-email");
        n.setTitle("Title");
        n.setMessage("Msg");

        doThrow(new RuntimeException("invalid address")).when(emailSenderService).sendEmail(any(), any(), any());

        assertThrows(PermanentFailureException.class, () -> dispatcher.dispatch(n));
    }

    @Test
    void testDispatchEmail_GenericSmtpException_Rethrows() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.EMAIL);
        n.setRecipientEmail("good@example.com");
        n.setTitle("Title");
        n.setMessage("Msg");

        doThrow(new RuntimeException("SMTP Server Down")).when(emailSenderService).sendEmail(any(), any(), any());

        assertThrows(RuntimeException.class, () -> dispatcher.dispatch(n));
    }

    // ── SMS ────────────────────────────────────────────────────────────────────

    @Test
    void testDispatchSms_Success() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.SMS);
        n.setRecipientPhone("+919000000000");
        n.setTitle("Alert");
        n.setMessage("Message");

        dispatcher.dispatch(n);

        verify(smsSenderService).sendSms("+919000000000", "Alert: Message");
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void testDispatchSms_ProviderDisabled() {
        ProviderConfigValidator.setSmsEnabled(false);

        Notification n = new Notification();
        n.setChannel(NotificationChannel.SMS);

        assertThrows(ProviderDisabledException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(smsSenderService);
    }

    @Test
    void testDispatchSms_MissingPhone_ThrowsPermanentFailure() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.SMS);
        n.setRecipientPhone(null);

        assertThrows(PermanentFailureException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(smsSenderService);
    }

    @Test
    void testDispatchSms_BlankPhone_ThrowsPermanentFailure() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.SMS);
        n.setRecipientPhone("  ");

        assertThrows(PermanentFailureException.class, () -> dispatcher.dispatch(n));
        verifyNoInteractions(smsSenderService);
    }

    // ── in-app ─────────────────────────────────────────────────────────────────

    @Test
    void testDispatchInApp_NoExternalCalls() {
        Notification n = new Notification();
        n.setChannel(NotificationChannel.IN_APP);

        assertDoesNotThrow(() -> dispatcher.dispatch(n));

        verifyNoInteractions(emailSenderService);
        verifyNoInteractions(smsSenderService);
    }
}
