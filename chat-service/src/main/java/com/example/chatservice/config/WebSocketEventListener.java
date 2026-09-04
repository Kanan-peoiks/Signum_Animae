package com.example.chatservice.config;

import com.example.chatservice.service.PresenceTypingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceTypingService presenceTypingService;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Long userId = extractUserId(event.getMessage());
        if (userId != null) {
            presenceTypingService.markOnline(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserIdFromAccessor(accessor);
        if (userId != null) {
            presenceTypingService.markOffline(userId);
        }
    }

    private Long extractUserId(org.springframework.messaging.Message<byte[]> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        return extractUserIdFromAccessor(accessor);
    }

    private Long extractUserIdFromAccessor(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object userId = sessionAttributes.get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
