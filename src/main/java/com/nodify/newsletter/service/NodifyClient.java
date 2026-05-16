package com.nodify.newsletter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodify.newsletter.dto.NodifyContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NodifyClient {

    @Value("${nodify.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public NodifyContent fetchContent(String contentCode, String titleFromPayload, String subjectFromPayload) {
        NodifyContent content = new NodifyContent();

        content.setTitle(titleFromPayload != null ? titleFromPayload : "Newsletter");
        content.setSubject(subjectFromPayload != null ? subjectFromPayload : "Newsletter");

        try {
            String htmlUrl = apiUrl + "/contents/code/" + contentCode + "?fillValues=true&payloadOnly=true";
            ResponseEntity<String> htmlResponse = restTemplate.exchange(htmlUrl, HttpMethod.GET, null, String.class);
            content.setMainHtml(htmlResponse.getBody());

            String jsonCode = contentCode.replace("HTML-", "JSON-");
            String jsonUrl = apiUrl + "/contents/code/" + jsonCode + "?fillValues=true";

            try {
                ResponseEntity<String> jsonResponse = restTemplate.exchange(jsonUrl, HttpMethod.GET, null,
                        String.class);
                JsonNode json = mapper.readTree(jsonResponse.getBody());

                if (json.has("title") && titleFromPayload == null) {
                    content.setTitle(json.get("title").asText());
                }
                if (json.has("subject") && subjectFromPayload == null) {
                    content.setSubject(json.get("subject").asText());
                }
            } catch (Exception e) {
                System.out.println("No JSON config found for: " + jsonCode);
            }

        } catch (Exception e) {
            System.err.println("Error fetching content: " + e.getMessage());
            content.setMainHtml("<html><body><h1>Error loading content</h1></body></html>");
        }

        return content;
    }
}