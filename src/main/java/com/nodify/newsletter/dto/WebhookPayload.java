package com.nodify.newsletter.dto;

import lombok.Data;
import java.util.List;

@Data
public class WebhookPayload {
    private String eventType;
    private ClientPayload clientPayload;

    @Data
    public static class ClientPayload {
        private String campaignCode;
        private String newsletterCode;
        private String contentCode;
        private String title;
        private String subject;
        private String scheduledStart;
        private String timestamp;
        private String startDate;
        private String endDate;
        private List<String> emails;
    }
}