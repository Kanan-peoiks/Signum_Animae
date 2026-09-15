package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens", indexes = @Index(name = "idx_auth_token_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthTokenType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean used = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.used == null) {
            this.used = false;
        }
    }

    public boolean isUsable() {
        return !Boolean.TRUE.equals(used) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
