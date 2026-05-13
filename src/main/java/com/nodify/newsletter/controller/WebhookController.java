package com.nodify.newsletter.controller;

import com.nodify.newsletter.dto.NodifyContent;
import com.nodify.newsletter.dto.WebhookPayload;
import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.service.CampaignService;
import com.nodify.newsletter.service.NewsletterService;
import com.nodify.newsletter.service.NodifyClient;
import com.nodify.newsletter.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Autowired
    private NodifyClient nodifyClient;

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerNewsletter(@RequestBody WebhookPayload payload) {
        String nodeCode = payload.getClientPayload().getCode();
        String campaignFolder = payload.getClientPayload().getFolder();

        NodifyContent content = nodifyClient.fetchContent(nodeCode);
        Newsletter newsletter = newsletterService.createFromNodifyContent(content);
        Campaign campaign = campaignService.create(newsletter, campaignFolder);
        schedulerService.startCampaign(campaign);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/track/{trackingId}")
    public void trackOpen(@PathVariable String trackingId) {
        UserNewsletterStatus status = statusRepository.findByTrackingId(trackingId).orElse(null);
        if (status != null && !status.getOpened()) {
            status.setOpened(true);
            status.setOpenedAt(java.time.LocalDateTime.now());
            status.setImpacted(true);
            statusRepository.save(status);
        }
    }
}