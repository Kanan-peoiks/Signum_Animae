package com.example.chatservice.service;

import com.example.chatservice.dto.ChatRoomRequest;
import com.example.chatservice.dto.ChatRoomResponse;
import com.example.chatservice.exception.ChatRoomNotFoundException;
import com.example.chatservice.model.ChatRoom;
import com.example.chatservice.repo.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * A chat room is created lazily the first time either side opens the chat
     * for a given booking. This method is idempotent: calling it again for the
     * same bookingId just returns the existing room instead of creating a duplicate.
     */
    public ChatRoomResponse getOrCreateRoom(ChatRoomRequest request) {
        ChatRoom room = chatRoomRepository.findByBookingId(request.getBookingId())
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .customerId(request.getCustomerId())
                                .artistId(request.getArtistId())
                                .bookingId(request.getBookingId())
                                .build()
                ));
        return ChatRoomResponse.fromEntity(room);
    }

    public ChatRoom findRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Söhbət otağı tapılmadı: " + roomId));
    }

    /** Raw entities (not DTOs) for internal use - e.g. ChatMessageService needs just the ids
     *  to compute an unread-message count across every room this user is part of. */
    public List<ChatRoom> findRoomsForUser(Long userId) {
        return chatRoomRepository.findByCustomerIdOrArtistId(userId, userId);
    }

    public ChatRoomResponse getRoom(Long roomId) {
        return ChatRoomResponse.fromEntity(findRoomOrThrow(roomId));
    }

    public List<ChatRoomResponse> getRoomsForCustomer(Long customerId) {
        return chatRoomRepository.findByCustomerId(customerId).stream()
                .map(ChatRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getRoomsForArtist(Long artistId) {
        return chatRoomRepository.findByArtistId(artistId).stream()
                .map(ChatRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
