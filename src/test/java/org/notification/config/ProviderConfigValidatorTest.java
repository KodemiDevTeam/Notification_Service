package org.notification.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigValidatorTest {

    @BeforeEach
    @AfterEach
    void resetFlags() {
        ReflectionTestUtils.setField(ProviderConfigValidator.class, "emailEnabled", true);
        ReflectionTestUtils.setField(ProviderConfigValidator.class, "smsEnabled", true);
    }

    @Test
    void validate_configuredMailAndTwilio_setsFlagsTrue() {
        ProviderConfigValidator validator = new ProviderConfigValidator();
        ReflectionTestUtils.setField(validator, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(validator, "mailPort", 587);
        ReflectionTestUtils.setField(validator, "mailUsername", "user@gmail.com");
        ReflectionTestUtils.setField(validator, "mailPassword", "secret");

        ReflectionTestUtils.setField(validator, "twilioAccountSid", "AC123");
        ReflectionTestUtils.setField(validator, "twilioAuthToken", "auth123");
        ReflectionTestUtils.setField(validator, "twilioPhoneNumber", "+1234567890");

        validator.validate();

        assertTrue(ProviderConfigValidator.isEmailEnabled());
        assertTrue(ProviderConfigValidator.isSmsEnabled());
    }

    @Test
    void validate_missingMailOrTwilio_setsFlagsFalse() {
        ProviderConfigValidator validator = new ProviderConfigValidator();
        ReflectionTestUtils.setField(validator, "mailHost", null);
        ReflectionTestUtils.setField(validator, "twilioAccountSid", null);

        validator.validate();

        assertFalse(ProviderConfigValidator.isEmailEnabled());
        assertFalse(ProviderConfigValidator.isSmsEnabled());
    }
}

