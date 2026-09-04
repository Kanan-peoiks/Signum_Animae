package com.example.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Ustanın "boşam" dediyi vaxt pəncərələri - sifariş yaradılmasını məcburi
 *  məhdudlaşdırmır (bookingDate hələ də sərbəst seçilir), sadəcə müştəriyə
 *  ustanın nə vaxt uyğun olduğunu göstərir - orientasiya üçün. */
@Entity
@Table(name = "availability_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long artistId;

    @Column(nullable = false)
    private LocalDateTime slotStart;

    @Column(nullable = false)
    private LocalDateTime slotEnd;

    /** Usta özü əl ilə "dolu" işarələyə bilər (avtomatik bağlanmır). */
    @Builder.Default
    private boolean booked = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
