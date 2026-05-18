package com.nodify.newsletter.repository;

import com.nodify.newsletter.model.Newsletter;
import com.nodify.newsletter.model.User;
import com.nodify.newsletter.model.UserNewsletterSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserNewsletterSubscriptionRepository extends JpaRepository<UserNewsletterSubscription, Long> {
    Page<UserNewsletterSubscription> findByNewsletter(Newsletter newsletter, Pageable pageable);

    List<UserNewsletterSubscription> findByNewsletter(Newsletter newsletter);

    boolean existsByNewsletterAndUser(Newsletter newsletter, User user);

    Optional<UserNewsletterSubscription> findByNewsletterAndUser(Newsletter newsletter, User user);

    void deleteByNewsletterAndUser(Newsletter newsletter, User user);
}