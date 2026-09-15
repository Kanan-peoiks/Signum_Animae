package com.example.chatservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long artistId;

    /** The tattoo request this room belongs to (Booking.id in booking-service). */
    @Column(nullable = false, unique = true)
    private Long bookingId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Bir otağın YALNIZ iki iştirakçısı var. Yoxlama modelin üzərindədir ki, həm
     * ChatMessageService, həm ChatRoomService eyni qaydadan istifadə etsin və qayda
     * iki yerdə ayrı-ayrı yazılmasın.
     *
     * null "iştirakçı deyil" sayılır: kimliyi məlum olmayan çağıran (məsələn gateway-in
     * X-User-Id başlığı gəlməyibsə) heç nəyə icazə almamalıdır.
     */
    public boolean isParticipant(Long userId) {
        return userId != null && (userId.equals(customerId) || userId.equals(artistId));
    }
}
