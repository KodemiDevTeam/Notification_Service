package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.exception.EmailSendingException;
import org.notification.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService emailService;

    @Test
    void sendEmail_shouldSendSuccessfully() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> emailService.sendEmail("to@example.com", "Subject", "Body"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldThrowWhenToIsNull() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail(null, "Subject", "Body"));
    }

    @Test
    void sendEmail_shouldThrowWhenToIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail("", "Subject", "Body"));
    }

    @Test
    void sendEmail_shouldThrowWhenSubjectIsNull() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail("to@example.com", null, "Body"));
    }

    @Test
    void sendEmail_shouldThrowWhenBodyIsNull() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmail("to@example.com", "Subject", null));
    }

    @Test
    void sendEmail_shouldThrowEmailSendingExceptionOnMailFailure() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThrows(EmailSendingException.class, () -> emailService.sendEmail("to@example.com", "Subject", "Body"));
    }
}
