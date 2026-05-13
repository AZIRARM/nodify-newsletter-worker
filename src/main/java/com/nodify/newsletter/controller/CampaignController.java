package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.service.SchedulerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/campaign")
public class CampaignController {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @Autowired
    private SchedulerService schedulerService;

    @GetMapping("/{id}")
    public String viewCampaign(@PathVariable Long id, Model model) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return "redirect:/";
        }

        List<UserNewsletterStatus> statuses = statusRepository.findByCampaignOrderBySentAtDesc(campaign);

        model.addAttribute("campaign", campaign);
        model.addAttribute("statuses", statuses);

        return "campaign-detail";
    }

    @GetMapping("/api/campaigns/{id}/stats")
    @ResponseBody
    public Map<String, Object> getCampaignStats(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        Map<String, Object> stats = new HashMap<>();
        long sent = statusRepository.countByCampaign(campaign);
        long opened = statusRepository.countOpenedByCampaign(campaign);
        stats.put("sentCount", sent);
        stats.put("openedCount", opened);
        stats.put("openRate", sent > 0 ? (opened * 100.0 / sent) : 0);
        return stats;
    }

    @GetMapping("/api/campaigns/{id}/users")
    @ResponseBody
    public Map<String, Object> getCampaignUsers(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UserNewsletterStatus> statusPage = statusRepository.findByCampaign(campaign, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("total", statusPage.getTotalElements());
        result.put("users", statusPage.getContent().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("email", s.getUser().getEmail());
            map.put("firstName", s.getUser().getFirstName());
            map.put("lastName", s.getUser().getLastName());
            map.put("sentAt", s.getSentAt());
            map.put("openedAt", s.getOpenedAt());
            map.put("opened", s.getOpened());
            return map;
        }).collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/api/campaigns/{id}/retry")
    @ResponseBody
    public void retryCampaign(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign != null && "COMPLETED".equals(campaign.getStatus())) {
            schedulerService.retryCampaign(campaign);
        }
    }

    @GetMapping("/campaigns")
    public String campaignsPage(Model model) {
        return "campaigns";
    }

    @GetMapping("/campaign/{id}")
    public String campaignDetail(@PathVariable Long id, Model model) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        model.addAttribute("campaign", campaign);
        return "campaign-detail";
    }

    @GetMapping("/campaign/{id}/schedule")
    public String scheduleForm(@PathVariable Long id, Model model) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        model.addAttribute("campaign", campaign);
        return "schedule-form";
    }
}