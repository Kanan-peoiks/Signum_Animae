package com.example.chatservice.dto;

import com.example.chatservice.model.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private Long customerId;
    private Long artistId;
    private Long bookingId;
    private LocalDateTime createdAt;

    public static ChatRoomResponse fromEntity(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .customerId(room.getCustomerId())
                .artistId(room.getArtistId())
                .bookingId(room.getBookingId())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
