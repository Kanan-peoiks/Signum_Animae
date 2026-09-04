package com.example.chatservice.service;

import com.example.chatservice.client.BookingServiceClient;
import com.example.chatservice.client.dto.BookingStatusDto;
import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.exception.BookingCancelledException;
import com.example.chatservice.exception.NotRoomParticipantException;
import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.ChatRoom;
import com.example.chatservice.model.MessageType;
import com.example.chatservice.repo.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the one thing that used to be enforced ONLY by hiding a button in the
 * frontend (see the earlier "ləğv olunmuş sifarişdə söhbət/təklif qadağası" feature):
 * a cancelled booking must actually reject a new OFFER server-side, not just look
 * blocked in the UI. Also covers that only a room's own customer/artist can act in it.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BookingServiceClient bookingServiceClient;

    private ChatMessageService service;

    private final ChatRoom room = ChatRoom.builder().id(1L).customerId(7L).artistId(3L).bookingId(50L).build();

    private void init() {
        service = new ChatMessageService(chatMessageRepository, chatRoomService, messagingTemplate, bookingServiceClient);
    }

    @Test
    void saveMessage_rejectsOffer_whenBookingIsCancelled() {
        init();
        when(chatRoomService.findRoomOrThrow(1L)).thenReturn(room);
        when(bookingServiceClient.getBookingStatus(50L)).thenReturn(new BookingStatusDto(50L, 7L, 3L, "CANCELLED"));

        ChatMessageRequest offer = new ChatMessageRequest();
        offer.setMessageType(MessageType.OFFER);
        offer.setContent("Yeni təklif");
        offer.setAmount(300.0);

        assertThatThrownBy(() -> service.saveMessage(1L, offer, 3L))
                .isInstanceOf(BookingCancelledException.class);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void saveMessage_allowsPlainTextMessage_evenWhenBookingIsCancelled() {
        init();
        when(chatRoomService.findRoomOrThrow(1L)).thenReturn(room);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        ChatMessageRequest text = new ChatMessageRequest();
        text.setMessageType(MessageType.TEXT);
        text.setContent("Salam, sifariş üçün üzr istəyirəm");

        service.saveMessage(1L, text, 7L);

        // Plain chat must keep working after a cancellation - only NEW OFFERS are
        // blocked - and the check for "is it cancelled" should never even run for a
        // TEXT message (no need to ask booking-service at all).
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verifyNoInteractions(bookingServiceClient);
    }

    @Test
    void saveMessage_rejectsSomeoneWhoIsNotARoomParticipant() {
        init();
        when(chatRoomService.findRoomOrThrow(1L)).thenReturn(room);

        ChatMessageRequest text = new ChatMessageRequest();
        text.setMessageType(MessageType.TEXT);
        text.setContent("Mən bu otağa aid deyiləm");

        assertThatThrownBy(() -> service.saveMessage(1L, text, 999L))
                .isInstanceOf(NotRoomParticipantException.class);
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void saveMessage_senderIdAlwaysComesFromVerifiedCaller_neverFromRequestBody() {
        init();
        when(chatRoomService.findRoomOrThrow(1L)).thenReturn(room);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageRequest spoofed = new ChatMessageRequest();
        spoofed.setMessageType(MessageType.TEXT);
        spoofed.setContent("Mən əslində sənəm deyə göndərirəm");
        spoofed.setSenderId(3L); // caller is 7 (the customer), but the payload claims to be 3 (the artist)

        service.saveMessage(1L, spoofed, 7L);

        var captor = org.mockito.ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getSenderId()).isEqualTo(7L); // the verified caller, not the spoofed value
    }
}
