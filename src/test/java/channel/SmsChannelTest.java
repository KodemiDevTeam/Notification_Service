package channel;

import org.notification.channel.SmsChannel;
import org.notification.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsChannelTest {

    private SmsChannel smsChannel;

    @BeforeEach
    void setUp() {
        // Provide dummy Twilio config values to satisfy constructor injection
        smsChannel = new SmsChannel("dummy-sid", "dummy-token", "+10000000000");
    }

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
    void send_shouldThrowWhenMessageIsEmpty() {
        Notification n = new Notification();
        n.setPhoneNumber("1234567890");
        // getMessage() returns "" which is blank, so InvalidMessageException is thrown
        assertThrows(RuntimeException.class, () -> smsChannel.send(n));
    }
}
