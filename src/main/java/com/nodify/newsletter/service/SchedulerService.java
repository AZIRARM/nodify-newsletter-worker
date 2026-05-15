package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
public class SchedulerService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;
    @Autowired
    private EmailService emailService;

    @Async
    @Transactional
    public void startCampaign(Campaign campaign) {
        Long campaignId = campaign.getId();
        LocalDateTime now = LocalDateTime.now();

        if (campaign.getStartDate() != null && now.isBefore(campaign.getStartDate())) {
            System.out.println(
                    "Campaign " + campaign.getName() + " not started yet. Start date: " + campaign.getStartDate());
            return;
        }

        if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
            System.out.println("Campaign " + campaign.getName() + " has expired. End date: " + campaign.getEndDate());
            campaign.setStatus("EXPIRED");
            campaignRepository.save(campaign);
            return;
        }

        System.out.println("Starting campaign: " + campaign.getName() + " (ID: " + campaignId + ")");
        campaign.setStatus("SENDING");
        campaignRepository.save(campaign);

        List<UserNewsletterStatus> campaignUsers = statusRepository.findByCampaign(campaign);
        System.out.println("Found " + campaignUsers.size() + " users linked to campaign");

        for (UserNewsletterStatus status : campaignUsers) {
            if (status.getSentAt() == null) {
                System.out.println("Sending to: " + status.getUser().getEmail());
                emailService.sendNewsletter(status.getUser(), campaign.getNewsletter(), campaignId);
            } else {
                System.out.println("Already sent to: " + status.getUser().getEmail());
            }
        }

        campaign.setStatus("COMPLETED");
        campaignRepository.save(campaign);
        System.out.println("Campaign completed: " + campaign.getName());
    }

    @Async
    @Transactional
    public void retryCampaign(Campaign campaign) {
        Long campaignId = campaign.getId();
        List<UserNewsletterStatus> nonOpened = statusRepository.findByCampaignAndOpenedFalse(campaign);

        System.out
                .println("Retrying campaign: " + campaign.getName() + " for " + nonOpened.size() + " non-opened users");

        for (UserNewsletterStatus status : nonOpened) {
            System.out.println("Retrying for: " + status.getUser().getEmail());
            emailService.sendNewsletter(status.getUser(), campaign.getNewsletter(), campaignId);
        }
    }

    @Scheduled(fixedDelayString = "${scheduler.check-interval-ms:60000}")
    public void checkScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        List<Campaign> toStart = campaignRepository.findByScheduledStartBeforeAndStatus(now, "SCHEDULED");
        for (Campaign campaign : toStart) {
            System.out.println("Starting scheduled campaign: " + campaign.getName());
            startCampaign(campaign);
        }

        List<Campaign> toRetry = campaignRepository.findByRetryDateTimeBeforeAndActiveTrue(now);
        for (Campaign campaign : toRetry) {
            System.out.println("Auto-retry for campaign: " + campaign.getName());
            retryCampaign(campaign);

            if (campaign.getRetryIntervalMinutes() != null && campaign.getRetryIntervalMinutes() > 0) {
                campaign.setRetryDateTime(now.plusMinutes(campaign.getRetryIntervalMinutes()));
                campaignRepository.save(campaign);
                System.out.println("Next retry scheduled at: " + campaign.getRetryDateTime());
            } else {
                campaign.setActive(false);
                campaignRepository.save(campaign);
                System.out.println("Auto-retry disabled for campaign: " + campaign.getName());
            }
        }
    }
}