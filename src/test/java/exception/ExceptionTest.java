package exception;

import org.junit.jupiter.api.Test;
import org.notification.exception.*;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void emailSendingException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        EmailSendingException ex = new EmailSendingException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void smsSendingException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        SmsSendingException ex = new SmsSendingException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void inAppNotificationException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        InAppNotificationException ex = new InAppNotificationException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void notificationNotFoundException_shouldHaveMessage() {
        NotificationNotFoundException ex = new NotificationNotFoundException("not found");
        assertEquals("not found", ex.getMessage());
    }

    @Test
    void notificationValidationException_shouldHaveMessage() {
        NotificationValidationException ex = new NotificationValidationException("invalid");
        assertEquals("invalid", ex.getMessage());
    }

    @Test
    void invalidAuthorizationException_shouldHaveMessage() {
        InvalidAuthorizationException ex = new InvalidAuthorizationException("unauthorized");
        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    void invalidEmailException_shouldHaveMessage() {
        InvalidEmailException ex = new InvalidEmailException("bad email");
        assertEquals("bad email", ex.getMessage());
    }

    @Test
    void invalidEmailException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        InvalidEmailException ex = new InvalidEmailException("bad email", cause);
        assertEquals("bad email", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void invalidPhoneNumberException_shouldHaveMessage() {
        InvalidPhoneNumberException ex = new InvalidPhoneNumberException("bad phone");
        assertEquals("bad phone", ex.getMessage());
    }

    @Test
    void invalidDeviceTokenException_shouldHaveMessage() {
        InvalidDeviceTokenException ex = new InvalidDeviceTokenException("bad token");
        assertEquals("bad token", ex.getMessage());
    }

    @Test
    void invalidDeviceTokenException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        InvalidDeviceTokenException ex = new InvalidDeviceTokenException("bad token", cause);
        assertEquals("bad token", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void invalidMessageException_shouldHaveMessage() {
        InvalidMessageException ex = new InvalidMessageException("bad message");
        assertEquals("bad message", ex.getMessage());
    }

    @Test
    void invalidUserIdException_shouldHaveMessage() {
        InvalidUserIdException ex = new InvalidUserIdException("bad userId");
        assertEquals("bad userId", ex.getMessage());
    }

    @Test
    void invalidUserIdException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        InvalidUserIdException ex = new InvalidUserIdException("bad userId", cause);
        assertEquals("bad userId", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
