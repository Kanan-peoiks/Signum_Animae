package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Integer experienceYears;
    private String styles;

    @Builder.Default
    private Double ratingAvg = 0.0;

    @Builder.Default
    private Integer ratingCount = 0;
}