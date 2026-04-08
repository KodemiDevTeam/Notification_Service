package channel;

import org.notification.channel.EmailChannel;
import org.notification.model.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChannelTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailChannel emailChannel;

    @Test
    void getChannelName_shouldReturnEMAIL() {
        assertEquals("EMAIL", emailChannel.getChannelName());
    }

    @Test
    void send_shouldSendEmail() {
        Notification n = new Notification();
        n.setEmail("user@example.com");
        n.setTitle("Test");
        n.setDescription("Hello");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailChannel.send(n));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_shouldThrowWhenEmailIsNull() {
        Notification n = new Notification();
        n.setEmail(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> emailChannel.send(n));
        assertEquals("Email is required for EMAIL channel", ex.getMessage());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_shouldThrowWhenEmailIsEmpty() {
        Notification n = new Notification();
        n.setEmail("");

        assertThrows(RuntimeException.class, () -> emailChannel.send(n));
    }
}
