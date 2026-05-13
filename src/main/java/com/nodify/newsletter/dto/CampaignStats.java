package com.nodify.newsletter.dto;

import lombok.Data;

@Data
public class CampaignStats {
    private Long campaignId;
    private String campaignName;
    private long totalSent;
    private long totalOpened;
    private double openRate;
}