package org.Notification.channel;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.Notification.model.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsChannel implements NotificationChannel {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @Override
    public String getChannelName() {
        return "SMS";
    }

    @Override
    public void send(Notification n) {

        // ✅ Validation
        if (n == null || n.getPhoneNumber() == null || n.getPhoneNumber().isEmpty()) {
            throw new RuntimeException("Phone number is required for SMS channel");
        }

        if (n.getMessage() == null || n.getMessage().isEmpty()) {
            throw new RuntimeException("Message content is required");
        }

        try {
            // ✅ Initialize Twilio (only once ideally, but okay here for now)
            Twilio.init(accountSid, authToken);

            // ✅ Send SMS
            Message message = Message.creator(
                    new PhoneNumber(n.getPhoneNumber()),   // TO
                    new PhoneNumber(fromNumber),           // FROM
                    n.getMessage()                         // MESSAGE
            ).create();

            System.out.println("✅ SMS sent successfully. SID: " + message.getSid());

        } catch (Exception e) {
            System.out.println(" Error sending SMS: " + e.getMessage());
            throw new RuntimeException("SMS sending failed", e);
        }
    }
}