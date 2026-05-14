package com.nodify.newsletter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodify.newsletter.repository.CampaignRepository;
import com.nodify.newsletter.repository.NewsletterRepository;
import com.nodify.newsletter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExportImportService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NewsletterRepository newsletterRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public byte[] exportAll() throws java.io.IOException {
        Map<String, Object> export = new HashMap<>();
        export.put("users", userRepository.findAll());
        export.put("newsletters", newsletterRepository.findAll());
        export.put("campaigns", campaignRepository.findAll());
        return mapper.writeValueAsBytes(export);
    }

    public void importAll(InputStream in) throws java.io.IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> importData = mapper.readValue(in, Map.class);
        System.out.println("Import not fully implemented - handle carefully");
    }
}