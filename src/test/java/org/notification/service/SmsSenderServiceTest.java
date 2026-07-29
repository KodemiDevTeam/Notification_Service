package org.notification.service;

import com.twilio.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.exception.PermanentFailureException;

import static org.junit.jupiter.api.Assertions.*;

class SmsSenderServiceTest {

    private SmsSenderService smsSenderService;

    @BeforeEach
    void setUp() {
        smsSenderService = new SmsSenderService("test-sid", "test-token", "+1234567890");
        smsSenderService.init();
    }

    @Test
    void testInit_realSid_doesNotThrow() {
        SmsSenderService realService = new SmsSenderService("REAL_SID_MOCK", "test-token", "+1234567890");
        assertDoesNotThrow(realService::init);
    }

    @Test
    void testSendSms_MockMode_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("+919000000001", "Hello"));
        assertDoesNotThrow(() -> smsSenderService.sendSms("919000000001", "Hello"));
        assertDoesNotThrow(() -> smsSenderService.sendSms("9000000001", "Hello"));
    }

    @Test
    void testSendSms_InvalidPhone_ThrowsException() {
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms("", "Hello"));
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms(null, "Hello"));
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms("123", "Hello"));
    }

    @Test
    void testSendSms_RealSid_Success() {
        SmsSenderService realService = new SmsSenderService("REAL_SID_MOCK", "test-token", "+1234567890") {
            @Override
            protected void sendTwilioMessage(String to, String body) {
                // simulate successful Twilio API call
            }
        };
        assertDoesNotThrow(() -> realService.sendSms("+919000000001", "Test msg"));
    }

    @Test
    void testSendSms_RealSid_TwilioUnverifiedError_ThrowsPermanentFailureException() {
        SmsSenderService realService = new SmsSenderService("REAL_SID_MOCK", "test-token", "+1234567890") {
            @Override
            protected void sendTwilioMessage(String to, String body) {
                throw new ApiException("The number is unverified");
            }
        };
        assertThrows(PermanentFailureException.class, () -> realService.sendSms("+919000000001", "Test msg"));
    }

    @Test
    void testSendSms_RealSid_TwilioGenericError_ThrowsRuntimeException() {
        SmsSenderService realService = new SmsSenderService("REAL_SID_MOCK", "test-token", "+1234567890") {
            @Override
            protected void sendTwilioMessage(String to, String body) {
                throw new ApiException("Rate limit exceeded");
            }
        };
        assertThrows(RuntimeException.class, () -> realService.sendSms("+919000000001", "Test msg"));
    }

    @Test
    void testSendSms_RealSid_GenericException_ThrowsRuntimeException() {
        SmsSenderService realService = new SmsSenderService("REAL_SID_MOCK", "test-token", "+1234567890") {
            @Override
            protected void sendTwilioMessage(String to, String body) {
                throw new RuntimeException("Network issue");
            }
        };
        assertThrows(RuntimeException.class, () -> realService.sendSms("+919000000001", "Test msg"));
    }
}
