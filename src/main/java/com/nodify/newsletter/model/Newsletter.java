package com.nodify.newsletter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Newsletter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String newsletterCode;
    private String title;
    private String description;
    @Column(columnDefinition = "TEXT")
    private String contentHtml;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}