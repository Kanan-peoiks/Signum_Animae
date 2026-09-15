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

    /**
     * A chat room is created lazily the first time either side opens the chat
     * for a given booking. This method is idempotent: calling it again for the
     * same bookingId just returns the existing room instead of creating a duplicate.
     *
     * İKİ ayrı yoxlama var və hər ikisi lazımdır:
     *  1. Çağıran iddia etdiyi iki tərəfdən biri olmalıdır - başqalarının adından otaq
     *     açmasın.
     *  2. Otaq ARTIQ varsa, gövdədəki id-lər deyil, otağın ÖZ id-ləri həlledicidir -
     *     əks halda kimsə öz id-lərini başqasının bookingId-si ilə göndərib həmin bronun
     *     tərəflərini öyrənə bilərdi (metod mövcud otağı qaytarır).
     */
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

    /** Otaq əməliyyatlarının ortaq qapısı - qayda ChatRoom.isParticipant-dədir. */
    public void requireParticipant(ChatRoom room, Long callerId) {
        if (!room.isParticipant(callerId)) {
            throw new NotRoomParticipantException(
                    "Bu söhbət sizə aid deyil, burada əməliyyat apara bilməzsiniz.");
        }
    }

    /** Yalnız öz məlumatına baxmaq olar - yoldakı id çağıranın id-si ilə üst-üstə düşməlidir. */
    public void requireSelf(Long pathUserId, Long callerId) {
        if (callerId == null || !callerId.equals(pathUserId)) {
            throw new NotRoomParticipantException("Yalnız öz söhbətlərinə baxa bilərsən.");
        }
    }

    public ChatRoom findRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Söhbət otağı tapılmadı: " + roomId));
    }

    /** Raw entities (not DTOs) for internal use - e.g. ChatMessageService needs just the ids
     *  to compute an unread-message count across every room this user is part of.
     *  Servis daxili istifadə olduğu üçün icazə yoxlaması yoxdur - çağıran metod özü
     *  artıq kimin adından işlədiyini bilir. */
    public List<ChatRoom> findRoomsForUser(Long userId) {
        return chatRoomRepository.findByCustomerIdOrArtistId(userId, userId);
    }

    /** Eyni məntiq, yalnız ustanın öz otaqları - bax getOfferStatsForArtist. */
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
