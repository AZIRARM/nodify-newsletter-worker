package com.nodify.newsletter.repository;

import com.nodify.newsletter.model.Campaign;
import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserNewsletterStatusRepository extends JpaRepository<UserNewsletterStatus, Long> {

    Optional<UserNewsletterStatus> findByTrackingId(String trackingId);

    boolean existsByCampaignAndUser(Campaign campaign, User user);

    long countByOpenedTrue();

    List<UserNewsletterStatus> findByCampaignAndOpenedFalse(Campaign campaign);

    @Query("SELECT s FROM UserNewsletterStatus s WHERE s.campaign = :campaign ORDER BY s.sentAt DESC")
    List<UserNewsletterStatus> findByCampaignOrderBySentAtDesc(@Param("campaign") Campaign campaign);

    @Query("SELECT COUNT(s) FROM UserNewsletterStatus s WHERE s.campaign = :campaign")
    long countByCampaign(@Param("campaign") Campaign campaign);

    @Query("SELECT COUNT(s) FROM UserNewsletterStatus s WHERE s.campaign = :campaign AND s.opened = true")
    long countOpenedByCampaign(@Param("campaign") Campaign campaign);

    Page<UserNewsletterStatus> findByCampaign(Campaign campaign, Pageable pageable);

    List<UserNewsletterStatus> findByCampaign(Campaign campaign);

    Optional<UserNewsletterStatus> findByCampaignAndUser(Campaign campaign, User user);

    @Query("SELECT COUNT(s) FROM UserNewsletterStatus s WHERE s.sentAt IS NOT NULL")
    long countSent();

    @Query("SELECT s FROM UserNewsletterStatus s WHERE s.campaign = :campaign AND s.sentAt IS NULL")
    List<UserNewsletterStatus> findByCampaignAndSentAtIsNull(@Param("campaign") Campaign campaign);

    @Query("SELECT COUNT(s) FROM UserNewsletterStatus s WHERE s.newsletter = :newsletter")
    long countByNewsletter(@Param("newsletter") Newsletter newsletter);

    @Query("SELECT COUNT(s) FROM UserNewsletterStatus s WHERE s.newsletter = :newsletter AND s.opened = true")
    long countOpenedByNewsletter(@Param("newsletter") Newsletter newsletter);
}