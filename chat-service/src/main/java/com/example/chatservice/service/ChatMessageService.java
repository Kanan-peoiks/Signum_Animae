package com.example.chatservice.service;

import com.example.chatservice.client.BookingServiceClient;
import com.example.chatservice.client.dto.BookingStatusDto;
import com.example.chatservice.client.dto.UpdateBookingPriceRequest;
import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.dto.ChatMessageResponse;
import com.example.chatservice.exception.BookingCancelledException;
import com.example.chatservice.exception.InvalidOfferException;
import com.example.chatservice.exception.NotRoomParticipantException;
import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.ChatRoom;
import com.example.chatservice.model.MessageType;
import com.example.chatservice.model.OfferStatus;
import com.example.chatservice.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BookingServiceClient bookingServiceClient;

    /**
     * Single entry point for sending a message, used by BOTH the WebSocket
     * controller and the plain REST fallback controller. It persists the
     * message and broadcasts it to /topic/rooms/{roomId} exactly once, so
     * callers must NOT broadcast again themselves.
     *
     * @param callerId the VERIFIED sender (X-User-Id from the gateway for REST, or the
     *                 JWT-verified id from the WS handshake) - request.getSenderId() is
     *                 never trusted, so nobody can send a message "as" someone else.
     */
    public ChatMessageResponse saveMessage(Long roomId, ChatMessageRequest request, Long callerId) {
        ChatRoom room = chatRoomService.findRoomOrThrow(roomId);
        requireParticipant(room, callerId);

        if (request.getMessageType() == MessageType.OFFER) {
            requireBookingNotCancelled(room.getBookingId(),
                    "Bu sifariş ləğv edilib - yeni qiymət təklifi göndərmək olmaz. Müştəri yenidən sifariş açmalıdır.");
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoomId(roomId)
                .senderId(callerId)
                .content(request.getContent())
                .messageType(request.getMessageType() == null ? MessageType.TEXT : request.getMessageType())
                .amount(request.getAmount())
                .build();

        return persistAndBroadcast(message);
    }

    /**
     * A customer accepts or rejects an artist's OFFER message. Accepting is what
     * makes chat-based price negotiation actually mean something: it calls
     * booking-service (via Feign) to update Booking.estimatedPrice to the agreed
     * amount, and posts a SYSTEM message into the room so both sides see the
     * outcome, instead of the OFFER just sitting there as decorative text.
     */
    public ChatMessageResponse respondToOffer(Long roomId, Long messageId, boolean accept, Long callerId) {
        ChatRoom room = chatRoomService.findRoomOrThrow(roomId);
        requireParticipant(room, callerId);

        ChatMessage offer = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidOfferException("Mesaj tapılmadı: " + messageId));

        if (!offer.getChatRoomId().equals(roomId)) {
            throw new InvalidOfferException("Bu mesaj bu otağa aid deyil.");
        }
        if (offer.getMessageType() != MessageType.OFFER) {
            throw new InvalidOfferException("Bu mesaj bir təklif (OFFER) deyil, ona cavab verilə bilməz.");
        }
        if (offer.getOfferStatus() != OfferStatus.PENDING) {
            throw new InvalidOfferException("Bu təklifə artıq cavab verilib: " + offer.getOfferStatus());
        }
        if (accept) {
            requireBookingNotCancelled(room.getBookingId(),
                    "Bu sifariş ləğv edilib - artıq bir təklifi qəbul etmək olmaz.");
        }

        offer.setOfferStatus(accept ? OfferStatus.ACCEPTED : OfferStatus.REJECTED);
        ChatMessage savedOffer = chatMessageRepository.save(offer);

        if (accept) {
            UpdateBookingPriceRequest priceRequest = new UpdateBookingPriceRequest();
            priceRequest.setEstimatedPrice(offer.getAmount());
            try {
                bookingServiceClient.updatePrice(room.getBookingId(), priceRequest);
            } catch (Exception ex) {
                // The offer itself is already saved as ACCEPTED - a booking-service
                // hiccup shouldn't undo that. Same "never let a downstream call roll
                // back what already succeeded" approach as ReviewService.createReview.
                log.error("Bron qiyməti yenilənmədi (bookingId={}, yeni qiymət={}): {}",
                        room.getBookingId(), offer.getAmount(), ex.getMessage(), ex);
            }
        }

        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoomId(roomId)
                .senderId(offer.getSenderId())
                .content(accept
                        ? "Təklif qəbul edildi: " + offer.getAmount() + " AZN"
                        : "Təklif rədd edildi.")
                .messageType(MessageType.SYSTEM)
                .build();
        persistAndBroadcast(systemMessage);

        return ChatMessageResponse.fromEntity(savedOffer);
    }

    public List<ChatMessageResponse> getHistory(Long roomId, Long callerId) {
        ChatRoom room = chatRoomService.findRoomOrThrow(roomId);
        requireParticipant(room, callerId);
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long roomId, Long readerId) {
        List<ChatMessage> unread = chatMessageRepository.findByChatRoomIdAndReadFalseAndSenderIdNot(roomId, readerId);
        unread.forEach(m -> m.setRead(true));
        chatMessageRepository.saveAll(unread);
    }

    /** Total unread messages (sent by the OTHER side) across every room this user is part of -
     *  backs the "Söhbətlər" nav badge, the same idea as notification-service's unread count. */
    public long getUnreadCountForUser(Long userId) {
        List<Long> roomIds = chatRoomService.findRoomsForUser(userId).stream()
                .map(ChatRoom::getId)
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            return 0;
        }
        return chatMessageRepository.countByChatRoomIdInAndReadFalseAndSenderIdNot(roomIds, userId);
    }

    /** Müvəqqəti olaraq söndürülüb (demo üçün) - əvvəlki kimi hər çağırana icazə verilir. */
    private void requireParticipant(ChatRoom room, Long callerId) {
        // no-op
    }

    private void requireBookingNotCancelled(Long bookingId, String message) {
        try {
            BookingStatusDto booking = bookingServiceClient.getBookingStatus(bookingId);
            if (booking != null && STATUS_CANCELLED.equals(booking.getStatus())) {
                throw new BookingCancelledException(message);
            }
        } catch (BookingCancelledException ex) {
            throw ex;
        } catch (Exception ex) {
            // booking-service unreachable: fail closed on the safe side (block the offer)
            // rather than silently allowing a price agreement we can't actually verify.
            log.error("Bronun statusu yoxlanılmadı (bookingId={}): {}", bookingId, ex.getMessage(), ex);
            throw new BookingCancelledException("Sifarişin statusu təsdiqlənə bilmədi, yenidən cəhd edin.");
        }
    }

    private ChatMessageResponse persistAndBroadcast(ChatMessage message) {
        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageResponse response = ChatMessageResponse.fromEntity(saved);
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getChatRoomId(), response);
        return response;
    }
}
