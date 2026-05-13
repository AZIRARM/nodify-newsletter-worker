package com.nodify.newsletter.service;

import com.nodify.newsletter.dto.NodifyContent;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.Translation;
import com.nodify.newsletter.repository.NewsletterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NewsletterService {

    @Autowired
    private NewsletterRepository newsletterRepository;

    public Newsletter createFromNodifyContent(NodifyContent content) {
        Newsletter newsletter = new Newsletter();
        newsletter.setTitle(content.getTitle());
        newsletter.setDescription(content.getDescription());
        newsletter.setSubject(content.getSubject());

        String finalHtml = "";
        if (content.getHeaderHtml() != null)
            finalHtml += content.getHeaderHtml();
        if (content.getMainHtml() != null)
            finalHtml += content.getMainHtml();
        if (content.getFooterHtml() != null)
            finalHtml += content.getFooterHtml();
        newsletter.setContentHtml(finalHtml);
        newsletter.setCreatedAt(LocalDateTime.now());

        Newsletter saved = newsletterRepository.save(newsletter);

        for (Map.Entry<String, Map<String, String>> entry : content.getTranslations().entrySet()) {
            Translation translation = new Translation();
            translation.setLanguage(entry.getKey());
            Map<String, String> values = entry.getValue();
            translation.setTitle(values.get("title"));
            translation.setDescription(values.get("description"));
            translation.setSubject(values.get("subject"));
            translation.setContentHtml(values.get("content"));
            translation.setNewsletter(saved);
            saved.getTranslations().add(translation);
        }

        return newsletterRepository.save(saved);
    }
}