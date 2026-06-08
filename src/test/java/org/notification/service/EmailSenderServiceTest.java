package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    void testSendEmail_EmptyRecipient_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> emailSenderService.sendEmail("", "Subject", "Body"));
        assertThrows(IllegalArgumentException.class, () -> emailSenderService.sendEmail(null, "Subject", "Body"));
    }

    @Test
    void testSendEmail_Failure_ThrowsException() {
        doThrow(new RuntimeException("Mail exception")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThrows(RuntimeException.class, () -> emailSenderService.sendEmail("test@example.com", "Subject", "Body"));
    }
}
