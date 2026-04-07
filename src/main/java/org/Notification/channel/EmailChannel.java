package org.Notification.channel;

import org.Notification.channel.NotificationChannel;
import org.Notification.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailChannel implements NotificationChannel {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void send(Notification n) {

        if (n.getEmail() == null || n.getEmail().isEmpty()) {
            throw new RuntimeException("Email is required for EMAIL channel");
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(n.getEmail());
        msg.setSubject("Notification");
        msg.setText(n.getMessage());

        mailSender.send(msg);

        System.out.println("Email sent to: " + n.getEmail());
    }
}
