package com.example.chatservice.dto;

import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.MessageType;
import com.example.chatservice.model.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private MessageType messageType;
    private Double amount;
    private OfferStatus offerStatus;
    private boolean read;
    private LocalDateTime createdAt;

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .amount(message.getAmount())
                .offerStatus(message.getOfferStatus())
                .read(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
