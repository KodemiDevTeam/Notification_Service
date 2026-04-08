package org.notification.channel;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.extern.slf4j.Slf4j;

import org.notification.model.Notification;
import org.notification.exception.InvalidPhoneNumberException;
import org.notification.exception.InvalidMessageException;
import org.notification.exception.SmsSendingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsChannel implements NotificationChannel {

    private static final String CHANNEL_NAME = "SMS";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    // Constructor Injection for @Value fields
    public SmsChannel(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromNumber) {

        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(Notification n) {

        if (n == null || n.getPhoneNumber() == null
                || n.getPhoneNumber().isBlank()) {

            log.error("Phone number is missing");

            throw new InvalidPhoneNumberException(
                    "Phone number is required for SMS channel"
            );
        }

        if (n.getMessage() == null
                || n.getMessage().isBlank()) {

            log.error("Message content is missing");

            throw new InvalidMessageException(
                    "Message content is required for SMS channel"
            );
        }

        try {

            log.info("Initializing Twilio");

            Twilio.init(accountSid, authToken);

            Message message = Message.creator(
                    new PhoneNumber(n.getPhoneNumber()),
                    new PhoneNumber(fromNumber),
                    n.getMessage()
            ).create();

            log.info("SMS sent successfully. SID: {}",
                    message.getSid());

        } catch (Exception e) {

            log.error("Error sending SMS", e);

            throw new SmsSendingException(
                    "SMS sending failed", e);
        }
    }
}