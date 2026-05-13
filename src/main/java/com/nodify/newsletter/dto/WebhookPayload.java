package com.nodify.newsletter.dto;

import lombok.Data;

@Data
public class WebhookPayload {
    private String eventType;
    private ClientPayload clientPayload;

    @Data
    public static class ClientPayload {
        private String code;
        private String folder;
        private Boolean ssg;
        private String timestamp;
    }
}