package com.nodify.newsletter.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_newsletter_subscription")
public class UserNewsletterSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Newsletter newsletter;

    private LocalDateTime subscribedAt;
}