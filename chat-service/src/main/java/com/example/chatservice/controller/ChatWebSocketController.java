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

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        Long verifiedUserId = sessionUserId(headerAccessor);
        if (verifiedUserId == null) {
            return;
        }
        chatMessageService.saveMessage(roomId, request, verifiedUserId);
    }

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
