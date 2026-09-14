package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Müştərinin izlədiyi usta. İstifadəçi/usta məlumatları auth-service-də yaşadığına görə
 *  bu cədvəl də burada - signum_animae_authservice bazasında.
 *
 *  Yalnız id-lər saxlanılır (User-ə @ManyToOne yox): izləmə siyahısı oxunanda onsuz da
 *  ArtistProfile-dan xülasə çəkilir, əlaqə qoysaq hər sorğuda artıq JOIN yaranardı. */
@Entity
@Table(
        name = "artist_follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_artist_follow_customer_artist",
                columnNames = {"customer_id", "artist_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
