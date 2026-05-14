package com.nodify.newsletter.dto;

import lombok.Data;

@Data
public class WebhookPayload {
    private String eventType;
    private ClientPayload clientPayload;

    @Data
    public static class ClientPayload {
        private String campaignCode;
        private String newsletterCode;
        private String contentCode;
        private String scheduledStart;
        private String timestamp;
    }
}