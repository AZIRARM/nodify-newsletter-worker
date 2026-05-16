package com.nodify.newsletter.repository;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByScheduledStartBeforeAndStatus(LocalDateTime date, String status);

    List<Campaign> findByRetryDateTimeBeforeAndActiveTrue(LocalDateTime date);

    Optional<Campaign> findByCampaignCode(String campaignCode);

    long countByNewsletter(Newsletter newsletter);
}