package com.nodify.newsletter.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "campaign")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String campaignCode; // ← identifiant unique métier
    private String name;
    private String folder;

    @ManyToOne
    private Newsletter newsletter;

    private LocalDateTime scheduledStart;
    private LocalDateTime retryDateTime;
    private Integer retryIntervalMinutes;
    private Boolean active = true;
    private String status;
    private LocalDateTime firstSentAt;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

}