package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/campaigns")
    public String campaignsPage() {
        return "campaigns";
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("campaignCount", campaignRepository.count());
        stats.put("sentCount", statusRepository.count());
        stats.put("openedCount", statusRepository.countByOpenedTrue());
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
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        List<UserNewsletterStatus> statuses = statusRepository.findByCampaign(campaign);
        statusRepository.deleteAll(statuses);
        campaignRepository.delete(campaign);
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @GetMapping("/api/newsletters")
    @ResponseBody
    public List<Newsletter> getNewsletters() {
        return newsletterRepository.findAll();
    }

    @GetMapping("/api/users/recent")
    @ResponseBody
    public List<Map<String, Object>> getRecentUsers(@RequestParam(defaultValue = "5") int limit) {
        return userRepository.findAll().stream()
                .limit(limit)
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("email", u.getEmail());
                    map.put("firstName", u.getFirstName());
                    map.put("lastName", u.getLastName());
                    map.put("createdAt", u.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }
}