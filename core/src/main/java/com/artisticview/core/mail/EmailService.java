package com.artisticview.core.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text email when SMTP is configured (spring.mail.host). Without SMTP (local dev) it logs the
 * message instead, so verification and reset links can be copied from the console.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.from:no-reply@localhost}")
    private String from;

    @Async
    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[email:dev] to={} subject=\"{}\"\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (RuntimeException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
