package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import com.nodify.newsletter.model.UserNewsletterSubscription;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserNewsletterSubscriptionRepository;
import com.nodify.newsletter.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/newsletters")
public class NewsletterController {

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    @Autowired
    private UserNewsletterSubscriptionRepository subscriptionRepository;

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

    @GetMapping("/{id}/manage-users")
    public String newsletterUsersPage(@PathVariable Long id, Model model) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        model.addAttribute("newsletter", newsletter);
        return "newsletter-users";
    }

    @GetMapping("/{id}/users")
    @ResponseBody
    public Map<String, Object> getNewsletterUsers(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        if (newsletter == null) {
            return Map.of("total", 0, "users", List.of());
        }
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UserNewsletterStatus> statusPage = statusRepository.findByNewsletter(newsletter, pageable);

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

    @GetMapping("/{id}/subscribers")
    @ResponseBody
    public Map<String, Object> getNewsletterSubscribers(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        if (newsletter == null) {
            return Map.of("total", 0, "users", List.of());
        }
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UserNewsletterSubscription> subscriptionPage = subscriptionRepository.findByNewsletter(newsletter,
                pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("total", subscriptionPage.getTotalElements());
        result.put("users", subscriptionPage.getContent().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getUser().getId());
            map.put("email", s.getUser().getEmail());
            map.put("firstName", s.getUser().getFirstName());
            map.put("lastName", s.getUser().getLastName());
            map.put("phone", s.getUser().getPhone());
            map.put("address", s.getUser().getAddress());
            return map;
        }).collect(Collectors.toList()));
        return result;
    }

    @GetMapping("/{id}/available-users")
    @ResponseBody
    public List<Map<String, Object>> getAvailableUsersForNewsletter(@PathVariable Long id) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        if (newsletter == null) {
            return List.of();
        }
        Set<Long> existingUserIds = subscriptionRepository.findByNewsletter(newsletter).stream()
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

    @PostMapping("/{id}/add-user")
    @ResponseBody
    public ResponseEntity<?> addUserToNewsletter(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        User user = userRepository.findById(body.get("userId")).orElse(null);

        if (newsletter == null || user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Newsletter or user not found"));
        }

        if (!subscriptionRepository.existsByNewsletterAndUser(newsletter, user)) {
            UserNewsletterSubscription subscription = new UserNewsletterSubscription();
            subscription.setNewsletter(newsletter);
            subscription.setUser(user);
            subscription.setSubscribedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @DeleteMapping("/{id}/remove-user")
    @ResponseBody
    public ResponseEntity<?> removeUserFromNewsletter(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Newsletter newsletter = newsletterRepository.findById(id).orElse(null);
        User user = userRepository.findById(body.get("userId")).orElse(null);

        if (newsletter == null || user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Newsletter or user not found"));
        }

        subscriptionRepository.deleteByNewsletterAndUser(newsletter, user);
        return ResponseEntity.ok().body(Map.of("success", true));
    }

}