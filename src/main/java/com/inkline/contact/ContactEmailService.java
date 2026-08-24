package com.inkline.contact;

import com.inkline.email.ResendEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContactEmailService {

    private final ResendEmailService emailService;
    private final String contactTo;

    public ContactEmailService(
            ResendEmailService emailService,
            @Value("${inkline.contact.to:${CONTACT_TO:}}") String contactTo) {

        this.emailService = emailService;
        this.contactTo = contactTo;
    }

    public void send(ContactRequest request) {

        if (contactTo == null || contactTo.isBlank()) {
            throw new IllegalStateException(
                    "Contact email is not configured."
            );
        }

        String text =
                "New message from Inkline Contact Us\n\n"
                + "Name: " + request.getName() + "\n"
                + "Email: " + request.getEmail() + "\n"
                + "Subject: " + request.getSubject() + "\n\n"
                + "Message:\n"
                + request.getMessage();

        emailService.send(
                contactTo,
                "[Inkline Contact] " + request.getSubject(),
                text
        );
    }
}
