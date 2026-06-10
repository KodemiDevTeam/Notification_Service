package org.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigValidatorTest {

    private ProviderConfigValidator buildAndValidate(
            String mailHost, String mailUser, String mailPass,
            String sid, String token, String phone) {
        ProviderConfigValidator v = new ProviderConfigValidator();
        ReflectionTestUtils.setField(v, "mailHost", mailHost);
        ReflectionTestUtils.setField(v, "mailUsername", mailUser);
        ReflectionTestUtils.setField(v, "mailPassword", mailPass);
        ReflectionTestUtils.setField(v, "twilioAccountSid", sid);
        ReflectionTestUtils.setField(v, "twilioAuthToken", token);
        ReflectionTestUtils.setField(v, "twilioPhoneNumber", phone);
        v.validate();
        return v;
    }

    @Test
    void testAllConfigured_BothEnabled() {
        buildAndValidate("smtp.example.com", "user", "pass", "AC123", "token", "+1234");
        assertTrue(ProviderConfigValidator.isEmailEnabled());
        assertTrue(ProviderConfigValidator.isSmsEnabled());
    }

    @Test
    void testMissingMailHost_EmailDisabled() {
        buildAndValidate(null, "user", "pass", "AC123", "token", "+1234");
        assertFalse(ProviderConfigValidator.isEmailEnabled());
        assertTrue(ProviderConfigValidator.isSmsEnabled());
    }

    @Test
    void testBlankMailUsername_EmailDisabled() {
        buildAndValidate("smtp.example.com", "  ", "pass", "AC123", "token", "+1234");
        assertFalse(ProviderConfigValidator.isEmailEnabled());
    }

    @Test
    void testMissingTwilioSid_SmsDisabled() {
        buildAndValidate("smtp.example.com", "user", "pass", null, "token", "+1234");
        assertTrue(ProviderConfigValidator.isEmailEnabled());
        assertFalse(ProviderConfigValidator.isSmsEnabled());
    }

    @Test
    void testMissingTwilioPhone_SmsDisabled() {
        buildAndValidate("smtp.example.com", "user", "pass", "AC123", "token", "");
        assertFalse(ProviderConfigValidator.isSmsEnabled());
    }

    @Test
    void testBothMissing_BothDisabled() {
        buildAndValidate(null, null, null, null, null, null);
        assertFalse(ProviderConfigValidator.isEmailEnabled());
        assertFalse(ProviderConfigValidator.isSmsEnabled());
    }
}
