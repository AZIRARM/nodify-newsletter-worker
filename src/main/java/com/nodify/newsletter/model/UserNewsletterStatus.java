package com.nodify.newsletter.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    @ManyToOne(cascade = CascadeType.ALL)
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