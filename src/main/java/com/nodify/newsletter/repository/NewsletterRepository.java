package com.nodify.newsletter.repository;

import com.nodify.newsletter.model.Newsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {
    Optional<Newsletter> findByNewsletterCode(String newsletterCode);
}