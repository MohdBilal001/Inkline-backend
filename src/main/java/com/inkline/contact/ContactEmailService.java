package com.inkline.contact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String contactTo;

    public ContactEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${inkline.contact.to:${spring.mail.username:}}") String contactTo) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.contactTo = contactTo;
    }

    public void send(ContactRequest request) {
        if (mailUsername == null || mailUsername.isBlank()
                || contactTo == null || contactTo.isBlank()) {
            throw new IllegalStateException("Contact email is not configured.");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailUsername);
        mail.setTo(contactTo);
        mail.setReplyTo(request.getEmail());
        mail.setSubject("[Inkline Contact] " + request.getSubject());
        mail.setText(
                "New message from Inkline Contact Us\n\n"
                + "Name: " + request.getName() + "\n"
                + "Email: " + request.getEmail() + "\n"
                + "Subject: " + request.getSubject() + "\n\n"
                + "Message:\n"
                + request.getMessage()
        );

        mailSender.send(mail);
    }
}
