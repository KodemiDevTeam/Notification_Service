package org.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.notification.exception.PermanentFailureException;

import static org.junit.jupiter.api.Assertions.*;

class SmsSenderServiceTest {

    private SmsSenderService smsSenderService;

    @BeforeEach
    void setUp() {
        smsSenderService = new SmsSenderService("test-sid", "test-token", "+1234567890");
        smsSenderService.init();
    }

    // ── Happy path ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"+919000000001", "919000000001", "9000000001", "+91 900-000-0001", "(+91)9000000001"})
    void testSendSms_ValidFormats_Success(String number) {
        assertDoesNotThrow(() -> smsSenderService.sendSms(number, "Hello"));
    }

    // ── Invalid phone numbers ───────────────────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "1234567890"})
    void testSendSms_InvalidPhone_ThrowsPermanentFailure(String number) {
        assertThrows(PermanentFailureException.class, () -> smsSenderService.sendSms(number, "Hello"));
    }

    // ── init() ──────────────────────────────────────────────────────────────────

    @Test
    void testInit_TestSid_NoException() {
        SmsSenderService svc = new SmsSenderService("test-sid", "test-token", "+1234567890");
        assertDoesNotThrow(svc::init);
    }
}
