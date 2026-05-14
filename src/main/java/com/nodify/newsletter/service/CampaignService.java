package com.nodify.newsletter.service;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    public Campaign findOrCreateByCode(String campaignCode) {
        return campaignRepository.findByCampaignCode(campaignCode)
                .orElseGet(() -> {
                    Campaign c = new Campaign();
                    c.setCampaignCode(campaignCode);
                    c.setName(campaignCode);
                    c.setActive(true);
                    return c;
                });
    }
}