package com.example.chatservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private OfferStatus offerStatus;

    @Builder.Default
    private boolean read = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messageType == null) {
            this.messageType = MessageType.TEXT;
        }
        if (this.messageType == MessageType.OFFER && this.offerStatus == null) {
            this.offerStatus = OfferStatus.PENDING;
        }
    }
}
