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

    @Value("${nodify.api.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public NodifyContent fetchContent(String nodeCode) {
        String url = apiUrl + "/contents/node/code/" + nodeCode + "?fillValues=true&withFiles=true";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiSecret);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        return parseResponse(response.getBody());
    }

    private NodifyContent parseResponse(String response) {
        NodifyContent content = new NodifyContent();

        try {
            JsonNode items = mapper.readTree(response);

            for (JsonNode item : items) {
                String type = item.has("type") ? item.get("type").asText() : "";
                String payload = item.has("payload") ? item.get("payload").asText() : null;
                boolean favorite = item.has("favorite") && item.get("favorite").asBoolean();
                String fileName = item.has("fileName") ? item.get("fileName").asText() : null;

                if ("HTML".equals(type)) {
                    if (favorite || "index.html".equals(fileName)) {
                        content.setMainHtml(payload);
                    } else if ("header.html".equals(fileName)) {
                        content.setHeaderHtml(payload);
                    } else if ("footer.html".equals(fileName)) {
                        content.setFooterHtml(payload);
                    }
                }

                if ("JSON".equals(type) && payload != null) {
                    JsonNode json = mapper.readTree(payload);
                    if (json.has("title"))
                        content.setTitle(json.get("title").asText());
                    if (json.has("description"))
                        content.setDescription(json.get("description").asText());
                    if (json.has("subject"))
                        content.setSubject(json.get("subject").asText());

                    if (json.has("translations")) {
                        JsonNode translations = json.get("translations");
                        for (JsonNode trans : translations) {
                            String lang = trans.get("language").asText();
                            String transTitle = trans.has("title") ? trans.get("title").asText() : null;
                            String transDesc = trans.has("description") ? trans.get("description").asText() : null;
                            String transSubject = trans.has("subject") ? trans.get("subject").asText() : null;
                            String transContent = trans.has("content") ? trans.get("content").asText() : null;
                            content.addTranslation(lang, transTitle, transDesc, transSubject, transContent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Nodify response: " + e.getMessage());
        }

        return content;
    }
}