package org.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.extern.slf4j.Slf4j;

import org.notification.config.TwilioConfig;
import org.notification.exception.SmsSendingException;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class SmsService {

    private final TwilioConfig twilioConfig;

    public SmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    @PostConstruct
    public void init() {
        Twilio.init(
                twilioConfig.getAccountSid(),
                twilioConfig.getAuthToken()
        );
        log.info("Twilio initialized successfully");
    }

    public void sendSms(String phone, String messageText) {

        if (phone == null || phone.isBlank()) {
            log.error("Phone number is missing");
            throw new IllegalArgumentException("Phone number is required");
        }

        if (messageText == null || messageText.isBlank()) {
            log.error("SMS message content is missing");
            throw new IllegalArgumentException("Message content is required");
        }

        try {
            log.info("Sending SMS to {}", phone);

            Message msg = Message.creator(
                    new PhoneNumber(phone),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    messageText
            ).create();

            log.info("SMS sent successfully. SID: {}", msg.getSid());

        } catch (Exception e) {
            log.error("SMS sending failed for {}", phone, e);
            throw new SmsSendingException("Failed to send SMS", e);
        }
    }
}
