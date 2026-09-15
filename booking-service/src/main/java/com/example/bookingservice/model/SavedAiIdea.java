package com.example.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_ai_ideas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedAiIdea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    private String style;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiRecommendation;

    private Long bookingId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
