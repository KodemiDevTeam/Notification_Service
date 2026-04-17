package config;

import org.junit.jupiter.api.Test;
import org.notification.config.TwilioConfig;

import static org.junit.jupiter.api.Assertions.*;

class TwilioConfigTest {

    @Test
    void getters_shouldReturnCorrectValues() {
        TwilioConfig config = new TwilioConfig("ACtest", "authtest", "+10000000000");
        assertEquals("ACtest", config.getAccountSid());
        assertEquals("authtest", config.getAuthToken());
        assertEquals("+10000000000", config.getPhoneNumber());
    }
}
