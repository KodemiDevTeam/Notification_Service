package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.config.TwilioConfig;
import org.notification.service.SmsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock TwilioConfig twilioConfig;
    @InjectMocks SmsService smsService;

    @Test
    void sendSms_shouldThrowWhenPhoneIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> smsService.sendSms(null, "Hello"));
    }

    @Test
    void sendSms_shouldThrowWhenPhoneIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> smsService.sendSms("", "Hello"));
    }

    @Test
    void sendSms_shouldThrowWhenMessageIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> smsService.sendSms("+91999", null));
    }

    @Test
    void sendSms_shouldThrowWhenMessageIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> smsService.sendSms("+91999", ""));
    }
}
