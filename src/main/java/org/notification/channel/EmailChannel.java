package org.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.notification.model.Notification;
import org.notification.exception.InvalidEmailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailChannel implements NotificationChannel {

    // Constant for channel name
    private static final String CHANNEL_NAME = "EMAIL";

    private final JavaMailSender mailSender;

    // Constructor Injection (instead of @Autowired)
    public EmailChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(Notification n) {

        // Null check added
        if (n == null || n.getEmail() == null
                || n.getEmail().isBlank()) {

            log.error("Email is missing in notification");

            throw new InvalidEmailException(
                    "Email is required for EMAIL channel"
            );
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(n.getEmail());
        msg.setSubject("Notification");
        msg.setText(n.getMessage());

        mailSender.send(msg);

        log.info("Email sent to: {}", n.getEmail());
    }
}