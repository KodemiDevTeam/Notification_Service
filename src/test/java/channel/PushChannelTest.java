package channel;

import org.notification.channel.PushChannel;
import org.notification.model.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PushChannelTest {

    private final PushChannel pushChannel = new PushChannel();

    @Test
    void getChannelName_shouldReturnPUSH() {
        assertEquals("PUSH", pushChannel.getChannelName());
    }

    @Test
    void send_shouldNotThrowWhenDeviceTokenPresent() {
        Notification n = new Notification();
        n.setUserId("user1");
        n.setDeviceToken("device-token-abc");
        n.setTitle("Test");
        n.setDescription("Hello");

        assertDoesNotThrow(() -> pushChannel.send(n));
    }

    @Test
    void send_shouldThrowWhenDeviceTokenIsNull() {
        Notification n = new Notification();
        n.setDeviceToken(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> pushChannel.send(n));
        assertEquals("Device token is required for PUSH channel", ex.getMessage());
    }

    @Test
    void send_shouldThrowWhenDeviceTokenIsEmpty() {
        Notification n = new Notification();
        n.setDeviceToken("");

        assertThrows(RuntimeException.class, () -> pushChannel.send(n));
    }
}
