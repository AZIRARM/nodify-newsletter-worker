package com.nodify.newsletter.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_newsletter_status")
public class UserNewsletterStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    private Newsletter newsletter;

    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private Boolean opened = false;
    private Boolean impacted = false;
    private String trackingId;
    private Boolean emailSent = false;
}