package org.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class ProviderConfigValidator {

    @Value("${spring.mail.host:#{null}}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private int mailPort;

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

    // Private static backing fields to avoid exposing mutable public static state
    private static boolean emailEnabled = true;
    private static boolean smsEnabled = true;

    @PostConstruct
    public void validate() {
        validateEmailProvider();
        validateSmsProvider();
    }

    private void validateEmailProvider() {
        if (isInvalid(mailHost) || mailPort <= 0 || isInvalid(mailUsername) || isInvalid(mailPassword)) {
            setEmailEnabled(false);
            log.warn("EMAIL_PROVIDER_DISABLED: Email configuration (host, valid port, username, or password) is missing or incomplete.");
        } else {
            setEmailEnabled(true);
            log.info("Email provider is configured successfully on host {} port {}.", mailHost, mailPort);
        }
    }

    private void validateSmsProvider() {
        if (isInvalid(twilioAccountSid) || isInvalid(twilioAuthToken) || isInvalid(twilioPhoneNumber)) {
            setSmsEnabled(false);
            log.warn("SMS_PROVIDER_DISABLED: Twilio configuration (accountSid, authToken, or phoneNumber) is missing.");
        } else {
            setSmsEnabled(true);
            log.info("SMS provider is configured successfully.");
        }
    }

    private boolean isInvalid(String val) {
        return val == null || val.isBlank();
    }

    // Thread-safe static setters to comply with SonarQube's static modification rules
    private static synchronized void setEmailEnabled(boolean enabled) {
        emailEnabled = enabled;
    }

    private static synchronized void setSmsEnabled(boolean enabled) {
        smsEnabled = enabled;
    }

    // Public static accessors so any service can check ProviderConfigValidator.isEmailEnabled()
    public static synchronized boolean isEmailEnabled() {
        return emailEnabled;
    }

    public static synchronized boolean isSmsEnabled() {
        return smsEnabled;
    }
}