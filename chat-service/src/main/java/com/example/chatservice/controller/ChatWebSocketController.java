package com.example.chatservice.controller;

import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.dto.TypingEventRequest;
import com.example.chatservice.dto.TypingEventResponse;
import com.example.chatservice.service.ChatMessageService;
import com.example.chatservice.service.PresenceTypingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final PresenceTypingService presenceTypingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends to: /app/rooms/{roomId}/send
     * Everyone subscribed to /topic/rooms/{roomId} receives the saved message.
     * No manual broadcast call needed here - saveMessage() already does it,
     * so both this and the REST fallback controller share one code path.
     *
     * The sender is the id PresenceHandshakeInterceptor verified against the client's
     * JWT at connect time (stored in the STOMP session attributes) - never whatever
     * request.getSenderId() the client payload claims.
     */
    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        Long verifiedUserId = sessionUserId(headerAccessor);
        if (verifiedUserId == null) {
            return; // handshake didn't verify an identity - nothing to trust, drop it.
        }
        chatMessageService.saveMessage(roomId, request, verifiedUserId);
    }

    /**
     * Client sends to: /app/rooms/{roomId}/typing
     * Everyone subscribed to /topic/rooms/{roomId}/typing gets notified.
     */
    @MessageMapping("/rooms/{roomId}/typing")
    public void typing(@DestinationVariable Long roomId, TypingEventRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
        Long verifiedUserId = sessionUserId(headerAccessor);
        if (verifiedUserId == null) {
            return;
        }
        presenceTypingService.markTyping(roomId, verifiedUserId);
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/typing",
                new TypingEventResponse(verifiedUserId, true)
        );
    }

    private Long sessionUserId(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            return null;
        }
        Object userId = headerAccessor.getSessionAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
