package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.exception.PermanentFailureException;
import org.notification.exception.SmsSendingException;

import static org.junit.jupiter.api.Assertions.*;

class SmsSenderServiceTest {

    private SmsSenderService smsSenderService;

    @BeforeEach
    void setUp() {
        smsSenderService = new SmsSenderService("test-sid", "test-token", "+1234567890");
        smsSenderService.init();
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    void testSendSms_E164Format_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("+919000000001", "Hello"));
    }

    @Test
    void testSendSms_IndianNumberWith91Prefix_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("919000000001", "Hello"));
    }

    @Test
    void testSendSms_IndianTenDigit_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("9000000001", "Hello"));
    }

    @Test
    void testSendSms_NumberWithSpacesAndHyphens_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("+91 900-000-0001", "Hello"));
    }

    @Test
    void testSendSms_NumberWithBrackets_Success() {
        assertDoesNotThrow(() -> smsSenderService.sendSms("(+91)9000000001", "Hello"));
    }

    // ── Invalid phone numbers ───────────────────────────────────────────────────

    @Test
    void testSendSms_NullPhone_ThrowsPermanentFailure() {
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms(null, "Hello"));
    }

    @Test
    void testSendSms_BlankPhone_ThrowsPermanentFailure() {
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms("", "Hello"));
    }

    @Test
    void testSendSms_TooShortNumber_ThrowsPermanentFailure() {
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms("123", "Hello"));
    }

    @Test
    void testSendSms_InvalidIndianNumber_ThrowsPermanentFailure() {
        // Starts with 1, not a valid Indian mobile prefix
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms("1234567890", "Hello"));
    }

    // ── init() in non-mock mode does not throw ──────────────────────────────────

    @Test
    void testInit_TestSid_NoException() {
        SmsSenderService svc = new SmsSenderService("test-sid", "test-token", "+1234567890");
        assertDoesNotThrow(svc::init);
    }
}
