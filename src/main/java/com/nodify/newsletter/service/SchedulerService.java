package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
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
    public void startCampaign(Campaign campaign) {
        campaign.setStatus("SENDING");
        campaignRepository.save(campaign);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean alreadySent = statusRepository.existsByCampaignAndUser(campaign, user);
            if (alreadySent)
                continue;

            String language = user.getLanguage() != null ? user.getLanguage() : "FR";
            emailService.sendNewsletter(user, campaign.getNewsletter(), campaign, language);
        }

        campaign.setStatus("COMPLETED");
        campaignRepository.save(campaign);
    }

    @Async
    public void retryCampaign(Campaign campaign) {
        List<UserNewsletterStatus> nonOpened = statusRepository.findByCampaignAndOpenedFalse(campaign);

        for (UserNewsletterStatus status : nonOpened) {
            User user = status.getUser();
            String language = user.getLanguage() != null ? user.getLanguage() : "FR";

            // Marquer l'ancien comme non impacté (si besoin)
            // Créer un nouveau status pour la relance
            emailService.sendNewsletter(user, campaign.getNewsletter(), campaign, language);
        }
    }

    @Scheduled(fixedDelayString = "${scheduler.check-interval-ms:60000}")
    public void checkScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        List<Campaign> toStart = campaignRepository.findByScheduledStartBeforeAndStatus(now, "SCHEDULED");
        for (Campaign campaign : toStart) {
            startCampaign(campaign);
        }

        List<Campaign> toRetry = campaignRepository.findByRetryDateTimeBeforeAndActiveTrue(now);
        for (Campaign campaign : toRetry) {
            retryCampaign(campaign);
            if (campaign.getRetryIntervalMinutes() != null) {
                campaign.setRetryDateTime(now.plusMinutes(campaign.getRetryIntervalMinutes()));
                campaignRepository.save(campaign);
            } else {
                campaign.setActive(false);
                campaignRepository.save(campaign);
            }
        }
    }
}