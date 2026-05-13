package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("campaignCount", campaignRepository.count());
        model.addAttribute("sentCount", statusRepository.count());
        model.addAttribute("openedCount", statusRepository.countByOpenedTrue());
        model.addAttribute("campaigns", campaignRepository.findAll());
        return "dashboard";
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("campaignCount", campaignRepository.count());
        stats.put("sentCount", statusRepository.count());
        stats.put("openedCount", statusRepository.countByOpenedTrue());
        stats.put("totalSent", statusRepository.count());
        return stats;
    }

    @GetMapping("/api/campaigns")
    @ResponseBody
    public List<Map<String, Object>> getCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Campaign c : campaigns) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("status", c.getStatus());
            long sent = statusRepository.countByCampaign(c);
            long opened = statusRepository.countOpenedByCampaign(c);
            map.put("sentCount", sent);
            map.put("openedCount", opened);
            map.put("openRate", sent > 0 ? (opened * 100 / sent) : 0);
            result.add(map);
        }
        return result;
    }

    @DeleteMapping("/api/campaigns/{id}")
    @ResponseBody
    public void deleteCampaign(@PathVariable Long id) {
        campaignRepository.deleteById(id);
    }

    @GetMapping("/api/newsletters")
    @ResponseBody
    public List<Newsletter> getNewsletters() {
        return newsletterRepository.findAll();
    }

    @PostMapping("/api/campaigns")
    @ResponseBody
    public void createCampaign(@RequestBody Map<String, Object> data) {
        String name = (String) data.get("name");
        String folder = (String) data.get("folder");
        Long newsletterId = Long.valueOf(data.get("newsletterId").toString());
        String scheduledStartStr = (String) data.get("scheduledStart");

        Newsletter newsletter = newsletterRepository.findById(newsletterId).orElse(null);
        Campaign campaign = new Campaign();
        campaign.setName(name);
        campaign.setFolder(folder);
        campaign.setNewsletter(newsletter);
        campaign.setStatus(scheduledStartStr != null && !scheduledStartStr.isEmpty() ? "SCHEDULED" : "SENDING");
        if (scheduledStartStr != null && !scheduledStartStr.isEmpty()) {
            campaign.setScheduledStart(LocalDateTime.parse(scheduledStartStr));
        }
        campaign.setActive(true);
        campaign.setFirstSentAt(LocalDateTime.now());
        campaignRepository.save(campaign);
    }

    @GetMapping("/campaigns")
    public String campaignsPage(Model model) {
        return "campaigns";
    }
}