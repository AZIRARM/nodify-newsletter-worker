package com.nodify.newsletter.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class NodifyContent {
    private String mainHtml;
    private String headerHtml;
    private String footerHtml;
    private String title;
    private String description;
    private String subject;
    private Map<String, Map<String, String>> translations = new HashMap<>();

    public void addTranslation(String language, String title, String description, String subject, String content) {
        Map<String, String> trans = new HashMap<>();
        trans.put("title", title);
        trans.put("description", description);
        trans.put("subject", subject);
        trans.put("content", content);
        translations.put(language, trans);
    }
}