package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.exception.EmailSendingException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailSenderService emailSenderService;

    @BeforeEach
    void setUp() {
        emailSenderService = new EmailSenderService(mailSender);
    }

    @Test
    void testSendEmail_Success() {
        emailSenderService.sendEmail("test@example.com", "Subject", "Body");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_NullRecipient_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> emailSenderService.sendEmail(null, "Subject", "Body"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void testSendEmail_BlankRecipient_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> emailSenderService.sendEmail("   ", "Subject", "Body"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void testSendEmail_EmptyRecipient_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> emailSenderService.sendEmail("", "Subject", "Body"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void testSendEmail_MailSenderThrows_WrapsAsEmailSendingException() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThrows(EmailSendingException.class,
                () -> emailSenderService.sendEmail("test@example.com", "Subject", "Body"));
    }
}
