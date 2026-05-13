package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.Translation;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @Value("${webhook.tracking-base-url}")
    private String trackingBaseUrl;

    public void sendNewsletter(User user, Newsletter newsletter, Campaign campaign, String language) {
        // Si le mail n'est pas configuré, on crée juste le statut sans envoyer d'email
        if (mailSender == null) {
            System.out.println("⚠️ Mail service not configured. Email not sent to: " + user.getEmail());
            UserNewsletterStatus status = new UserNewsletterStatus();
            status.setUser(user);
            status.setCampaign(campaign);
            status.setNewsletter(newsletter);
            status.setSentAt(LocalDateTime.now());
            status.setTrackingId(UUID.randomUUID().toString());
            status.setOpened(false);
            status.setImpacted(false);
            statusRepository.save(status);
            return;
        }

        try {
            Translation translation = newsletter.getTranslations().stream()
                    .filter(t -> t.getLanguage().equals(language))
                    .findFirst()
                    .orElse(null);

            String subject = translation != null ? translation.getSubject() : newsletter.getSubject();
            String content = translation != null ? translation.getContentHtml() : newsletter.getContentHtml();

            String trackingId = UUID.randomUUID().toString();
            String trackingPixel = "<img src='" + trackingBaseUrl + "/webhook/track/" + trackingId
                    + "' width='1' height='1' />";
            String finalContent = content + trackingPixel;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(finalContent, true);

            mailSender.send(message);

            UserNewsletterStatus status = new UserNewsletterStatus();
            status.setUser(user);
            status.setCampaign(campaign);
            status.setNewsletter(newsletter);
            status.setSentAt(LocalDateTime.now());
            status.setTrackingId(trackingId);
            status.setOpened(false);
            status.setImpacted(false);
            statusRepository.save(status);

        } catch (Exception e) {
            System.err.println("Failed to send email to " + user.getEmail() + ": " + e.getMessage());
        }
    }
}