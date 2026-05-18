package com.nodify.newsletter.controller;

import com.nodify.newsletter.dto.NodifyContent;
import com.nodify.newsletter.dto.WebhookPayload;
import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.service.CampaignService;
import com.nodify.newsletter.service.NewsletterService;
import com.nodify.newsletter.service.NodifyClient;
import com.nodify.newsletter.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${webhook.secret}")
    private String webhookSecret;

    @Autowired
    private NodifyClient nodifyClient;
    @Autowired
    private NewsletterService newsletterService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private SchedulerService schedulerService;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerNewsletter(@RequestBody WebhookPayload payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (authorization == null || !authorization.equals("Bearer " + webhookSecret)) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
        }

        String campaignCode = payload.getClientPayload().getCampaignCode();
        String newsletterCode = payload.getClientPayload().getNewsletterCode();
        String contentCode = payload.getClientPayload().getContentCode();
        String scheduledStartStr = payload.getClientPayload().getScheduledStart();
        String startDateStr = payload.getClientPayload().getStartDate();
        String endDateStr = payload.getClientPayload().getEndDate();

        // newsletterCode et contentCode sont obligatoires
        if (newsletterCode == null || newsletterCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newsletterCode is required"));
        }
        if (contentCode == null || contentCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "contentCode is required"));
        }

        String title = payload.getClientPayload().getTitle();
        String subject = payload.getClientPayload().getSubject();

        // Récupérer ou créer la newsletter
        Newsletter newsletter = newsletterService.findOrCreateByCode(newsletterCode);
        NodifyContent content = nodifyClient.fetchContent(contentCode, title, subject);
        newsletter = newsletterService.updateFromNodifyContent(newsletter, content);

        // Si campaignCode est présent, créer ou mettre à jour la campagne
        if (campaignCode != null && !campaignCode.isEmpty()) {
            Campaign campaign = campaignService.findOrCreateByCode(campaignCode);
            campaign.setNewsletter(newsletter);
            campaign.setName(campaignCode);
            campaign.setCampaignCode(campaignCode);

            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    campaign.setStartDate(LocalDateTime.parse(startDateStr));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid startDate format"));
                }
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    campaign.setEndDate(LocalDateTime.parse(endDateStr));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid endDate format"));
                }
            }

            if (scheduledStartStr != null && !scheduledStartStr.isEmpty()) {
                try {
                    campaign.setScheduledStart(LocalDateTime.parse(scheduledStartStr));
                    campaign.setStatus("SCHEDULED");
                    campaignRepository.save(campaign);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid scheduledStart format"));
                }
            } else {
                campaign.setStatus("SENDING");
                campaignRepository.save(campaign);
                schedulerService.startCampaign(campaign);
            }

            return ResponseEntity.ok().body(Map.of(
                    "campaignId", campaign.getId(),
                    "campaignCode", campaign.getCampaignCode(),
                    "newsletterId", newsletter.getId(),
                    "newsletterCode", newsletter.getNewsletterCode()));
        }

        // Pas de campagne, juste la newsletter
        return ResponseEntity.ok().body(Map.of(
                "newsletterId", newsletter.getId(),
                "newsletterCode", newsletter.getNewsletterCode()));
    }

    @GetMapping("/track/{trackingId}")
    public void trackOpen(@PathVariable String trackingId) {
        UserNewsletterStatus status = statusRepository.findByTrackingId(trackingId).orElse(null);
        if (status != null && !status.getOpened()) {
            status.setOpened(true);
            status.setOpenedAt(LocalDateTime.now());
            status.setImpacted(true);
            statusRepository.save(status);
        }
    }
}