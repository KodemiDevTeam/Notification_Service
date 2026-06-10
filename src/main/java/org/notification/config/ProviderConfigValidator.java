package org.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Validates provider configuration at startup and exposes enabled flags via
 * static accessor methods backed by instance state to satisfy SonarQube's
 * "make enclosing method static or remove this set" rule.
 */
@Slf4j
@Configuration
public class ProviderConfigValidator {

    @Value("${spring.mail.host:#{null}}")
    private String mailHost;

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    @Value("${spring.mail.password:#{null}}")
    private String mailPassword;

    @Value("${twilio.account-sid:#{null}}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:#{null}}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number:#{null}}")
    private String twilioPhoneNumber;

    // Holder carries instance state; static accessors read from it after Spring wires it.
    private static ProviderConfigValidator instance;

    private boolean emailEnabled = true;
    private boolean smsEnabled = true;

    public static boolean isEmailEnabled() {
        return instance == null || instance.emailEnabled;
    }

    public static boolean isSmsEnabled() {
        return instance == null || instance.smsEnabled;
    }

    @PostConstruct
    public void validate() {
        instance = this;

        if (isBlankAny(mailHost, mailUsername, mailPassword)) {
            emailEnabled = false;
            log.warn("EMAIL_PROVIDER_DISABLED: Email configuration (host, username, or password) is missing.");
        } else {
            log.info("Email provider is configured successfully.");
        }

        if (isBlankAny(twilioAccountSid, twilioAuthToken, twilioPhoneNumber)) {
            smsEnabled = false;
            log.warn("SMS_PROVIDER_DISABLED: Twilio configuration (accountSid, authToken, or phoneNumber) is missing.");
        } else {
            log.info("SMS provider is configured successfully.");
        }
    }

    private static boolean isBlankAny(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) return true;
        }
        return false;
    }
}
