package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.exception.PermanentFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmsSenderServiceTest {

    private SmsSenderService smsSenderService;

    @BeforeEach
    void setUp() {
        smsSenderService = new SmsSenderService("test-sid", "test-token", "+1234567890");
        smsSenderService.init();
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
}
