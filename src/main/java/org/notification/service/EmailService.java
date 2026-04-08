package org.notification.service;

import lombok.extern.slf4j.Slf4j;

import org.notification.exception.EmailSendingException;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    // Constructor Injection (Sonar preferred)
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(
            String to,
            String subject,
            String body) {

        // ==============================
        // INPUT VALIDATION
        // ==============================

        if (to == null || to.isBlank()) {

            log.error("Email 'to' address is missing");

            throw new IllegalArgumentException(
                    "Recipient email address is required"
            );
        }

        if (subject == null || subject.isBlank()) {

            log.error("Email subject is missing");

            throw new IllegalArgumentException(
                    "Email subject is required"
            );
        }

        if (body == null || body.isBlank()) {

            log.error("Email body is missing");

            throw new IllegalArgumentException(
                    "Email body is required"
            );
        }

        try {

            log.info("Sending EMAIL to {}", to);

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);

        } catch (Exception e) {

            log.error(
                    "EMAIL sending failed for {}",
                    to,
                    e
            );

            throw new EmailSendingException(
                    "Failed to send email",
                    e
            );
        }
    }
}