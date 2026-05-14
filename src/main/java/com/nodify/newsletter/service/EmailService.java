package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
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

    @Autowired
    private CampaignRepository campaignRepository;

    @Value("${webhook.tracking-base-url}")
    private String trackingBaseUrl;

    public void sendNewsletter(User user, Newsletter newsletter, Long campaignId) {
        if (mailSender == null) {
            System.out.println("⚠️ Mail service not configured. Email not sent to: " + user.getEmail());
            return;
        }

        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            System.err.println("Campaign not found: " + campaignId);
            return;
        }

        String trackingId = UUID.randomUUID().toString();
        String trackingPixel = "<img src='" + trackingBaseUrl + "/webhook/track/" + trackingId
                + "' width='1' height='1' />";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject(newsletter.getSubject());
            helper.setText(newsletter.getContentHtml() + trackingPixel, true);
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
            e.printStackTrace();
        }
    }
}