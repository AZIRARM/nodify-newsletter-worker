package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
import com.nodify.newsletter.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/campaigns")
public class CampaignController {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;
    @Autowired
    private SchedulerService schedulerService;
    @Autowired
    private NewsletterRepository newsletterRepository;

    // ==================== PAGES HTML ====================

    @GetMapping
    public String campaignsPage(Model model) {
        return "campaigns";
    }

    @GetMapping("/{id}/detail")
    public String campaignDetail(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignRepository.findById(id).orElse(null));
        return "campaign-detail";
    }

    @GetMapping("/{id}/schedule-form")
    public String scheduleForm(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignRepository.findById(id).orElse(null));
        return "schedule-form";
    }

    // ==================== API REST ====================

    @GetMapping("/list")
    @ResponseBody
    public List<Map<String, Object>> getCampaigns() {
        return campaignRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("status", c.getStatus());
            map.put("campaignStatus", computeCampaignStatus(c));
            map.put("startDate", c.getStartDate());
            map.put("endDate", c.getEndDate());
            map.put("scheduledStart", c.getScheduledStart());
            map.put("retryIntervalMinutes", c.getRetryIntervalMinutes());
            map.put("active", c.getActive());

            // ✅ Calcul réel des statistiques (même logique que /{id}/stats)
            List<UserNewsletterStatus> statuses =
                    statusRepository.findByCampaign(c, Pageable.unpaged()).getContent();
            long sent = statuses.stream().filter(s -> s.getSentAt() != null).count();
            long opened = statuses.stream().filter(s -> Boolean.TRUE.equals(s.getOpened())).count();
            long openRate = sent > 0 ? Math.round((opened * 100.0) / sent) : 0;

            map.put("sentCount", sent);
            map.put("openedCount", opened);
            map.put("openRate", openRate);

            map.put("newsletterId", c.getNewsletter() != null ? c.getNewsletter().getId() : null);
            map.put("newsletterTitle", c.getNewsletter() != null ? c.getNewsletter().getTitle() : null);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getCampaign(@PathVariable Long id) {
        Campaign c = campaignRepository.findById(id).orElse(null);
        if (c == null) return Map.of();
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("status", c.getStatus());
        map.put("startDate", c.getStartDate());
        map.put("endDate", c.getEndDate());
        map.put("scheduledStart", c.getScheduledStart());
        map.put("retryIntervalMinutes", c.getRetryIntervalMinutes());
        map.put("active", c.getActive());
        return map;
    }

    @GetMapping("/{id}/stats")
    @ResponseBody
    public Map<String, Object> getCampaignStats(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) return Map.of("sentCount", 0, "openedCount", 0);
        List<UserNewsletterStatus> statuses = statusRepository.findByCampaign(campaign, Pageable.unpaged()).getContent();
        long sent = statuses.stream().filter(s -> s.getSentAt() != null).count();
        long opened = statuses.stream().filter(s -> Boolean.TRUE.equals(s.getOpened())).count();
        Map<String, Object> result = new HashMap<>();
        result.put("sentCount", sent);
        result.put("openedCount", opened);
        return result;
    }

    @GetMapping("/{id}/users")
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
            map.put("id", s.getUser().getId());
            map.put("email", s.getUser().getEmail());
            map.put("firstName", s.getUser().getFirstName());
            map.put("lastName", s.getUser().getLastName());
            map.put("phone", s.getUser().getPhone());
            map.put("address", s.getUser().getAddress());
            map.put("sentAt", s.getSentAt());
            map.put("openedAt", s.getOpenedAt());
            map.put("opened", s.getOpened());
            return map;
        }).collect(Collectors.toList()));
        return result;
    }

    @PostMapping
    @ResponseBody
    public void createCampaign(@RequestBody Map<String, Object> data) {
        String name = (String) data.get("name");
        Long newsletterId = Long.valueOf(data.get("newsletterId").toString());
        String startDateStr = (String) data.get("startDate");
        String endDateStr = (String) data.get("endDate");
        String scheduledStartStr = (String) data.get("scheduledStart");

        Newsletter newsletter = newsletterRepository.findById(newsletterId).orElse(null);
        Campaign campaign = new Campaign();
        campaign.setName(name);
        campaign.setFolder(name);
        campaign.setNewsletter(newsletter);

        if (startDateStr != null && !startDateStr.isEmpty()) {
            campaign.setStartDate(LocalDateTime.parse(startDateStr));
        }
        if (endDateStr != null && !endDateStr.isEmpty()) {
            campaign.setEndDate(LocalDateTime.parse(endDateStr));
        }
        if (scheduledStartStr != null && !scheduledStartStr.isEmpty()) {
            campaign.setScheduledStart(LocalDateTime.parse(scheduledStartStr));
            campaign.setStatus("SCHEDULED");
        } else {
            campaign.setStatus("SENDING");
        }

        campaign.setActive(true);
        campaign.setFirstSentAt(LocalDateTime.now());
        campaignRepository.save(campaign);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }

        // ✅ Supprimer d'abord les statuts liés (contrainte FK)
        List<UserNewsletterStatus> statuses =
                statusRepository.findByCampaign(campaign, Pageable.unpaged()).getContent();
        statusRepository.deleteAll(statuses);

        // ✅ Puis supprimer la campagne
        campaignRepository.delete(campaign);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/schedule")
    public String updateSchedule(@PathVariable Long id,
            @RequestParam(required = false) String scheduledStart,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer retryIntervalMinutes,
            @RequestParam(required = false) Boolean active) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign != null) {
            if (scheduledStart != null && !scheduledStart.isEmpty()) {
                campaign.setScheduledStart(LocalDateTime.parse(scheduledStart));
                campaign.setStatus("SCHEDULED");
            }
            if (startDate != null && !startDate.isEmpty()) {
                campaign.setStartDate(LocalDateTime.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                campaign.setEndDate(LocalDateTime.parse(endDate));
            }
            if (retryIntervalMinutes != null) {
                campaign.setRetryIntervalMinutes(retryIntervalMinutes);
                if (retryIntervalMinutes > 0) {
                    campaign.setRetryDateTime(LocalDateTime.now().plusMinutes(retryIntervalMinutes));
                }
            }
            if (active != null) {
                campaign.setActive(active);
            }
            campaignRepository.save(campaign);
        }
        return "redirect:/api/campaigns/" + id + "/detail";
    }

    @PostMapping("/{id}/retry")
    @ResponseBody
    public ResponseEntity<?> retryCampaign(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        schedulerService.retryCampaign(campaign);
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    private String computeCampaignStatus(Campaign c) {
        LocalDateTime now = LocalDateTime.now();
        if (c.getStartDate() != null && c.getStartDate().isAfter(now)) return "scheduled";
        if (c.getEndDate() != null && c.getEndDate().isBefore(now)) return "inactive";
        return "active";
    }
}