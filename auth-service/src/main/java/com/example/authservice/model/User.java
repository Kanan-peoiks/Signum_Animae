package com.example.authservice.model;


import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;
    private String profileImageUrl;

    /** Nullable on purpose (not a primitive boolean): a plain "ADD COLUMN ... NOT NULL"
     *  would fail against an existing non-empty table under ddl-auto=update. Null is
     *  treated as "not banned" everywhere this is read - see AuthService.login and
     *  AdminService. */
    private Boolean banned;

    /** banned ile eyni səbəbdən nullable: mövcud dolu cədvələ NOT NULL sütun əlavə etmək
     *  ddl-auto=update altında sınardı. null = "hələ təsdiqlənməyib" - girişi BLOKLAMIR,
     *  yalnız profildə nişan kimi göstərilir (bax AuthService.register). */
    private Boolean emailVerified;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
