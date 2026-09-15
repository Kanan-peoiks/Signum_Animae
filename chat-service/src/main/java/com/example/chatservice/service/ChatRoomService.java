package com.example.chatservice.service;

import com.example.chatservice.dto.ChatRoomRequest;
import com.example.chatservice.dto.ChatRoomResponse;
import com.example.chatservice.exception.ChatRoomNotFoundException;
import com.example.chatservice.exception.NotRoomParticipantException;
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

    public ChatRoomResponse getOrCreateRoom(ChatRoomRequest request, Long callerId) {
        if (callerId == null
                || (!callerId.equals(request.getCustomerId()) && !callerId.equals(request.getArtistId()))) {
            throw new NotRoomParticipantException("Başqasının adından söhbət aça bilməzsən.");
        }

        ChatRoom room = chatRoomRepository.findByBookingId(request.getBookingId())
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .customerId(request.getCustomerId())
                                .artistId(request.getArtistId())
                                .bookingId(request.getBookingId())
                                .build()
                ));

        requireParticipant(room, callerId);
        return ChatRoomResponse.fromEntity(room);
    }

    public void requireParticipant(ChatRoom room, Long callerId) {
        if (!room.isParticipant(callerId)) {
            throw new NotRoomParticipantException(
                    "Bu söhbət sizə aid deyil, burada əməliyyat apara bilməzsiniz.");
        }
    }

    public void requireSelf(Long pathUserId, Long callerId) {
        if (callerId == null || !callerId.equals(pathUserId)) {
            throw new NotRoomParticipantException("Yalnız öz söhbətlərinə baxa bilərsən.");
        }
    }

    public ChatRoom findRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Söhbət otağı tapılmadı: " + roomId));
    }

    public List<ChatRoom> findRoomsForUser(Long userId) {
        return chatRoomRepository.findByCustomerIdOrArtistId(userId, userId);
    }

    public List<ChatRoom> findRoomsWhereArtist(Long artistId) {
        return chatRoomRepository.findByArtistId(artistId);
    }

    public ChatRoomResponse getRoom(Long roomId, Long callerId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room, callerId);
        return ChatRoomResponse.fromEntity(room);
    }

    public List<ChatRoomResponse> getRoomsForCustomer(Long customerId, Long callerId) {
        requireSelf(customerId, callerId);
        return chatRoomRepository.findByCustomerId(customerId).stream()
                .map(ChatRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatRoomResponse> getRoomsForArtist(Long artistId, Long callerId) {
        requireSelf(artistId, callerId);
        return chatRoomRepository.findByArtistId(artistId).stream()
                .map(ChatRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
