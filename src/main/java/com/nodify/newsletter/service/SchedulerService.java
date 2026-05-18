package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.model.UserNewsletterSubscription;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserNewsletterSubscriptionRepository;
import com.nodify.newsletter.repository.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@EnableScheduling
public class SchedulerService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserNewsletterSubscriptionRepository subscriptionRepository;

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

        List<UserNewsletterSubscription> subscribers = subscriptionRepository
                .findByNewsletter(campaign.getNewsletter());

        for (UserNewsletterSubscription subscription : subscribers) {
            Optional<UserNewsletterStatus> existing = statusRepository.findByCampaignAndUser(campaign,
                    subscription.getUser());
            if (existing.isEmpty() || existing.get().getSentAt() == null) {
                // Créer le status avant d'envoyer
                UserNewsletterStatus status = new UserNewsletterStatus();
                status.setUser(subscription.getUser());
                status.setCampaign(campaign);
                status.setNewsletter(campaign.getNewsletter());
                status.setSentAt(LocalDateTime.now());
                status.setOpened(false);
                status.setImpacted(false);
                status.setTrackingId(UUID.randomUUID().toString());
                statusRepository.save(status);

                emailService.sendNewsletter(subscription.getUser(), campaign.getNewsletter());
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

        List<UserNewsletterStatus> notSent = statusRepository.findByCampaignAndSentAtIsNull(campaign);

        for (UserNewsletterStatus status : notSent) {
            status.setSentAt(LocalDateTime.now());
            status.setTrackingId(UUID.randomUUID().toString());
            statusRepository.save(status);

            emailService.sendNewsletter(status.getUser(), campaign.getNewsletter());
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