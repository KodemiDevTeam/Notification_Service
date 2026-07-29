package org.notification.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.notification.exception.PermanentFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsSenderService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsSenderService(
            @Value("${twilio.account-sid:test-sid}") String accountSid,
            @Value("${twilio.auth-token:test-token}") String authToken,
            @Value("${twilio.phone-number:+1234567890}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @PostConstruct
    public void init() {
        try {
            if (!"test-sid".equals(accountSid)) {
                Twilio.init(accountSid, authToken);
                log.info("Twilio initialized successfully");
            } else {
                log.warn("Twilio running in MOCK mode because account SID is test-sid");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Twilio. SMS sending will fail until correct credentials are provided.", e);
        }
    }

    public void sendSms(String to, String body) {
        String normalizedTo = normalizePhoneNumber(to);

        if (normalizedTo == null) {
            log.warn("Skipping SMS because phone number is invalid: {}", to);
            throw new PermanentFailureException("Invalid phone number format: " + to);
        }

        if ("test-sid".equals(accountSid)) {
            log.info("MOCK SMS SENT to {}: {}", normalizedTo, body);
            return;
        }

        try {
            sendTwilioMessage(normalizedTo, body);
        } catch (ApiException e) {
            log.error("Twilio ApiException while sending SMS to {}", normalizedTo, e);
            String errorMsg = e.getMessage().toLowerCase();
            
            // Handle unverified number, invalid phone number, country permission
            if (errorMsg.contains("unverified") || 
                errorMsg.contains("invalid") || 
                errorMsg.contains("permission") ||
                errorMsg.contains("not enabled")) {
                throw new PermanentFailureException("Permanent Twilio Error: " + e.getMessage(), e);
            }
            // For other API exceptions (like timeout, server error), throw a standard RuntimeException to retry
            throw new RuntimeException("Temporary SMS sending failed", e);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", normalizedTo, e);
            throw new RuntimeException("SMS sending failed", e);
        }
    }

    protected void sendTwilioMessage(String to, String body) {
        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromNumber),
                body
        ).create();
        log.info("SMS sent successfully to {}. SID: {}", to, message.getSid());
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String cleaned = phoneNumber.trim();

        // Remove spaces, hyphens, brackets
        cleaned = cleaned.replaceAll("[\\s\\-()]", "");

        // Already E.164 format (+91938120XXXX)
        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        // Indian number starting with 91 but missing + (91938120XXXX)
        if (cleaned.matches("^91[6-9]\\d{9}$")) {
            return "+" + cleaned;
        }

        // Indian number with 10 digits (938120XXXX)
        if (cleaned.matches("^[6-9]\\d{9}$")) {
            return "+91" + cleaned;
        }

        return null; // Invalid
    }
}