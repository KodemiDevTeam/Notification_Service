package org.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Validates provider configuration at startup and exposes thread-safe
 * enabled flags through static accessors.
 * The flags are set once during {@code @PostConstruct} and are read-only thereafter.
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

    private static volatile boolean emailEnabled = true;
    private static volatile boolean smsEnabled = true;

    public static boolean isEmailEnabled() {
        return emailEnabled;
    }

    public static boolean isSmsEnabled() {
        return smsEnabled;
    }

    /** Package-visible for testing only. */
    static void setEmailEnabled(boolean value) {
        emailEnabled = value;
    }

    /** Package-visible for testing only. */
    static void setSmsEnabled(boolean value) {
        smsEnabled = value;
    }

    @PostConstruct
    public void validate() {
        setEmailEnabled(!isBlankAny(mailHost, mailUsername, mailPassword));
        setSmsEnabled(!isBlankAny(twilioAccountSid, twilioAuthToken, twilioPhoneNumber));

        if (!emailEnabled) {
            log.warn("EMAIL_PROVIDER_DISABLED: Email configuration (host, username, or password) is missing.");
        } else {
            log.info("Email provider is configured successfully.");
        }

        if (!smsEnabled) {
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
