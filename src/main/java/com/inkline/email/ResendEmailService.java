package com.inkline.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    private final Resend resend;
    private final String from;

    public ResendEmailService(
            @Value("${RESEND_API_KEY}") String apiKey,
            @Value("${MAIL_FROM}") String from) {

        this.resend = new Resend(apiKey);
        this.from = from;
    }

    public void send(String to, String subject, String text) {

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(text)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);

            if (response == null || response.getId() == null) {
                throw new IllegalStateException(
                        "Resend did not return an email ID."
                );
            }

        } catch (Exception e) {
            System.err.println("Failed to send email through Resend: " + e.getMessage());
        }
    }
}
