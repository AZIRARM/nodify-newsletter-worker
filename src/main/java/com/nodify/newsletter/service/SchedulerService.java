package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
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
    private UserNewsletterStatusRepository statusRepository;
    @Autowired
    private EmailService emailService;

    @Async
    @Transactional
    public void startCampaign(Campaign campaign) {
        Long campaignId = campaign.getId();
        LocalDateTime now = LocalDateTime.now();

        if (campaign.getStartDate() != null && now.isBefore(campaign.getStartDate())) {
            return;
        }

        if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
            campaign.setStatus("EXPIRED");
            campaignRepository.save(campaign);
            return;
        }

        campaign.setStatus("SENDING");
        campaignRepository.save(campaign);

        List<UserNewsletterStatus> campaignUsers = statusRepository.findByCampaign(campaign);

        for (UserNewsletterStatus status : campaignUsers) {
            if (status.getSentAt() == null) {
                emailService.sendNewsletter(status.getUser(), campaign.getNewsletter(), campaignId);
            }
        }

        campaign.setStatus("COMPLETED");
        campaignRepository.save(campaign);
    }

    @Async
    @Transactional
    public void retryCampaign(Campaign campaign) {
        Long campaignId = campaign.getId();
        LocalDateTime now = LocalDateTime.now();

        if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
            campaign.setActive(false);
            campaignRepository.save(campaign);
            return;
        }

        List<UserNewsletterStatus> nonOpened = statusRepository.findByCampaignAndOpenedFalse(campaign);

        for (UserNewsletterStatus status : nonOpened) {
            emailService.sendNewsletter(status.getUser(), campaign.getNewsletter(), campaignId);
        }
    }

    @Scheduled(fixedDelayString = "${scheduler.check-interval-ms:60000}")
    public void checkScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        List<Campaign> toStart = campaignRepository.findByScheduledStartBeforeAndStatus(now, "SCHEDULED");
        for (Campaign campaign : toStart) {
            if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
                campaign.setStatus("EXPIRED");
                campaignRepository.save(campaign);
                continue;
            }

            if (campaign.getStartDate() != null && now.isBefore(campaign.getStartDate())) {
                continue;
            }

            startCampaign(campaign);
        }

        List<Campaign> toRetry = campaignRepository.findByRetryDateTimeBeforeAndActiveTrue(now);
        for (Campaign campaign : toRetry) {
            if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
                campaign.setActive(false);
                campaignRepository.save(campaign);
                continue;
            }

            retryCampaign(campaign);

            if (campaign.getRetryIntervalMinutes() != null && campaign.getRetryIntervalMinutes() > 0) {
                campaign.setRetryDateTime(now.plusMinutes(campaign.getRetryIntervalMinutes()));
                campaignRepository.save(campaign);
            } else {
                campaign.setActive(false);
                campaignRepository.save(campaign);
            }
        }
    }
}