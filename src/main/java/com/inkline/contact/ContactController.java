package com.inkline.contact;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactEmailService contactEmailService;

    public ContactController(ContactEmailService contactEmailService) {
        this.contactEmailService = contactEmailService;
    }

    @PostMapping
    public ResponseEntity<?> sendContactMessage(
            @Valid @RequestBody ContactRequest request) {

        try {
            contactEmailService.send(request);
            return ResponseEntity.ok(
                    Map.of("message", "Your message has been sent successfully.")
            );
        } catch (MailException | IllegalStateException ex) {
            ex.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of("message", "We couldn't send your message right now. Please try again later.")
            );
        }
    }
}
