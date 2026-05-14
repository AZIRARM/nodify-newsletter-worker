package com.nodify.newsletter.service;

import com.nodify.newsletter.dto.NodifyContent;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.repository.NewsletterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NewsletterService {

    @Autowired
    private NewsletterRepository newsletterRepository;

    public Newsletter findOrCreateByCode(String newsletterCode) {
        return newsletterRepository.findByNewsletterCode(newsletterCode)
                .orElseGet(() -> {
                    Newsletter n = new Newsletter();
                    n.setNewsletterCode(newsletterCode);
                    n.setCreatedAt(LocalDateTime.now());
                    return n;
                });
    }

    public Newsletter updateFromNodifyContent(Newsletter newsletter, NodifyContent content) {
        newsletter.setTitle(content.getTitle());
        newsletter.setSubject(content.getSubject());
        newsletter.setContentHtml(content.getMainHtml());
        newsletter.setUpdatedAt(LocalDateTime.now());
        return newsletterRepository.save(newsletter);
    }
}