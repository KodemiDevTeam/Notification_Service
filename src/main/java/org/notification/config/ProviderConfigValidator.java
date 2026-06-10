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

    private static final boolean[] FLAGS = {true, true}; // [0]=emailEnabled, [1]=smsEnabled

    public static boolean isEmailEnabled() {
        return FLAGS[0];
    }

    public static boolean isSmsEnabled() {
        return FLAGS[1];
    }

    @PostConstruct
    public void validate() {
        if (mailHost == null || mailHost.isBlank()
                || mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank()) {
            FLAGS[0] = false;
            log.warn("EMAIL_PROVIDER_DISABLED: Email configuration (host, username, or password) is missing.");
        } else {
            log.info("Email provider is configured successfully.");
        }

        if (twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()
                || twilioPhoneNumber == null || twilioPhoneNumber.isBlank()) {
            FLAGS[1] = false;
            log.warn("SMS_PROVIDER_DISABLED: Twilio configuration (accountSid, authToken, or phoneNumber) is missing.");
        } else {
            log.info("SMS provider is configured successfully.");
        }
    }
}
