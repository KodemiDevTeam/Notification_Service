package channel;

import org.Notification.channel.SmsChannel;
import org.Notification.model.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsChannelTest {

    private final SmsChannel smsChannel = new SmsChannel();

    @Test
    void getChannelName_shouldReturnSMS() {
        assertEquals("SMS", smsChannel.getChannelName());
    }

    @Test
    void send_shouldThrowWhenPhoneNumberIsNull() {
        Notification n = new Notification();
        n.setPhoneNumber(null);
        n.setTitle("Test");
        n.setDescription("Hello");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> smsChannel.send(n));
        assertEquals("Phone number is required for SMS channel", ex.getMessage());
    }

    @Test
    void send_shouldThrowWhenPhoneNumberIsEmpty() {
        Notification n = new Notification();
        n.setPhoneNumber("");
        n.setTitle("Test");
        n.setDescription("Hello");

        assertThrows(RuntimeException.class, () -> smsChannel.send(n));
    }

    @Test
    void send_shouldThrowWhenMessageIsNull() {
        Notification n = new Notification();
        n.setPhoneNumber("1234567890");
        // getMessage() returns "" so this won't throw for null but for empty
        assertThrows(RuntimeException.class, () -> smsChannel.send(n));
    }
}
