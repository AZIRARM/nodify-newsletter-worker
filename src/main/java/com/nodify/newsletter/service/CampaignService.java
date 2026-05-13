package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    public Campaign create(Newsletter newsletter, String folder) {
        Campaign campaign = new Campaign();
        campaign.setName(folder);
        campaign.setFolder(folder);
        campaign.setNewsletter(newsletter);
        campaign.setStatus("SCHEDULED");
        campaign.setActive(true);
        campaign.setFirstSentAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public Campaign findById(Long id) {
        return campaignRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        campaignRepository.deleteById(id);
    }
}