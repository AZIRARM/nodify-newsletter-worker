package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.repository.CampaignRepository;
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
public class CampaignController {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserNewsletterStatusRepository statusRepository;
    @Autowired
    private SchedulerService schedulerService;

    @GetMapping("/campaign/{id}")
    public String campaignDetail(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignRepository.findById(id).orElse(null));
        return "campaign-detail";
    }

    @GetMapping("/campaign/{id}/schedule")
    public String scheduleForm(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignRepository.findById(id).orElse(null));
        return "schedule-form";
    }

    @PostMapping("/campaign/{id}/schedule")
    public String updateSchedule(@PathVariable Long id,
            @RequestParam(required = false) String scheduledStart,
            @RequestParam(required = false) Integer retryIntervalMinutes,
            @RequestParam(required = false) Boolean active) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign != null) {
            if (scheduledStart != null && !scheduledStart.isEmpty()) {
                campaign.setScheduledStart(LocalDateTime.parse(scheduledStart));
                campaign.setStatus("SCHEDULED");
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
        return "redirect:/campaign/" + id;
    }

    @GetMapping("/campaign/{id}/users")
    public String manageUsers(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignRepository.findById(id).orElse(null));
        return "campaign-users";
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

    @GetMapping("/api/campaigns/{id}/available-users")
    @ResponseBody
    public List<Map<String, Object>> getAvailableUsers(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        Set<Long> existingUserIds = statusRepository.findByCampaign(campaign).stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        return userRepository.findAll().stream()
                .filter(u -> !existingUserIds.contains(u.getId()))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("email", u.getEmail());
                    map.put("firstName", u.getFirstName());
                    map.put("lastName", u.getLastName());
                    map.put("phone", u.getPhone());
                    map.put("address", u.getAddress());
                    return map;
                }).collect(Collectors.toList());
    }

    @PostMapping("/api/campaigns/{id}/add-user")
    @ResponseBody
    public ResponseEntity<?> addUserToCampaign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        User user = userRepository.findById(body.get("userId")).orElse(null);

        if (campaign == null || user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campaign or user not found"));
        }

        if (!statusRepository.existsByCampaignAndUser(campaign, user)) {
            UserNewsletterStatus status = new UserNewsletterStatus();
            status.setCampaign(campaign);
            status.setUser(user);
            status.setNewsletter(campaign.getNewsletter());
            status.setOpened(false);
            status.setImpacted(false);
            statusRepository.save(status);
        }
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @DeleteMapping("/api/campaigns/{id}/remove-user")
    @ResponseBody
    public ResponseEntity<?> removeUserFromCampaign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (campaign == null || user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campaign or user not found"));
        }

        Optional<UserNewsletterStatus> status = statusRepository.findByCampaignAndUser(campaign, user);
        if (status.isPresent()) {
            statusRepository.delete(status.get());
        }
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @GetMapping("/api/campaigns/{id}/users-with-status")
    @ResponseBody
    public List<Map<String, Object>> getCampaignUsersWithStatus(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        List<UserNewsletterStatus> statuses = statusRepository.findByCampaign(campaign);
        Set<Long> userIds = statuses.stream().map(s -> s.getUser().getId()).collect(Collectors.toSet());

        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("inCampaign", userIds.contains(u.getId()));
            map.put("opened", statuses.stream().filter(s -> s.getUser().getId().equals(u.getId())).findFirst()
                    .map(UserNewsletterStatus::getOpened).orElse(false));
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/api/campaigns/{id}/retry")
    @ResponseBody
    public ResponseEntity<?> retryCampaign(@PathVariable Long id) {
        System.out.println("Retry endpoint called for campaign: " + id);
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        schedulerService.retryCampaign(campaign);
        return ResponseEntity.ok().body(Map.of("success", true));
    }
}