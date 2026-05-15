package com.nodify.newsletter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserNewsletterStatusRepository;
import com.nodify.newsletter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportImportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserNewsletterStatusRepository statusRepository;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public byte[] exportAll() throws java.io.IOException {
        Map<String, Object> export = new HashMap<>();
        export.put("users", userRepository.findAll());
        export.put("newsletters", newsletterRepository.findAll());
        export.put("campaigns", campaignRepository.findAll());
        export.put("statuses", statusRepository.findAll());
        return mapper.writeValueAsBytes(export);
    }

    public void importAll(InputStream in) throws java.io.IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> importData = mapper.readValue(in, Map.class);

        if (importData.containsKey("users")) {
            List<User> users = mapper.convertValue(importData.get("users"), new TypeReference<List<User>>() {
            });
            userRepository.saveAll(users);
            System.out.println("Imported " + users.size() + " users");
        }

        if (importData.containsKey("newsletters")) {
            List<Newsletter> newsletters = mapper.convertValue(importData.get("newsletters"),
                    new TypeReference<List<Newsletter>>() {
                    });
            newsletterRepository.saveAll(newsletters);
            System.out.println("Imported " + newsletters.size() + " newsletters");
        }

        if (importData.containsKey("campaigns")) {
            List<Campaign> campaigns = mapper.convertValue(importData.get("campaigns"),
                    new TypeReference<List<Campaign>>() {
                    });
            campaignRepository.saveAll(campaigns);
            System.out.println("Imported " + campaigns.size() + " campaigns");
        }
    }
}