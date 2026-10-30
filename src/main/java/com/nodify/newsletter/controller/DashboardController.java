package com.nodify.newsletter.controller;

import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private NewsletterRepository newsletterRepository;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    // ==================== PAGE HTML ====================

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("campaignCount", campaignRepository.count());
        model.addAttribute("sentCount", statusRepository.count());
        model.addAttribute("openedCount", statusRepository.countByOpenedTrue());
        model.addAttribute("campaigns", campaignRepository.findAll());
        return "dashboard";
    }

    // ==================== API REST ====================

    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("campaignCount", campaignRepository.count());
        stats.put("sentCount", statusRepository.count());
        stats.put("openedCount", statusRepository.countByOpenedTrue());
        return stats;
    }
}