package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/newsletters")
public class NewsletterController {

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @GetMapping
    public String newslettersPage(Model model) {
        return "newsletters";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public List<Map<String, Object>> getNewsletters() {
        return newsletterRepository.findAll().stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("code", n.getNewsletterCode());
            map.put("title", n.getTitle());
            map.put("subject", n.getSubject());
            map.put("createdAt", n.getCreatedAt());
            map.put("updatedAt", n.getUpdatedAt());

            long campaignCount = campaignRepository.countByNewsletter(n);
            map.put("campaignCount", campaignCount);
            map.put("canDelete", campaignCount == 0);

            return map;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public Map<String, Object> deleteNewsletter(@PathVariable Long id) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        if (newsletter == null) {
            return Map.of("error", "Newsletter not found");
        }

        long campaignCount = campaignRepository.countByNewsletter(newsletter);
        if (campaignCount > 0) {
            return Map.of("error", "Cannot delete: newsletter is used by " + campaignCount + " campaign(s)");
        }

        newsletterRepository.deleteById(id);
        return Map.of("success", true);
    }
}