package org.Notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.Notification.config.TwilioConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Autowired
    private TwilioConfig twilioConfig;

    public void sendSms(String phone, String message) {

        // Initialize Twilio
        Twilio.init(
                twilioConfig.getAccountSid(),
                twilioConfig.getAuthToken()
        );

        // Send SMS
        Message msg = Message.creator(
                new com.twilio.type.PhoneNumber(phone), // To
                new com.twilio.type.PhoneNumber(twilioConfig.getPhoneNumber()), // From
                message
        ).create();

        System.out.println("SMS sent successfully. SID: " + msg.getSid());
    }
}