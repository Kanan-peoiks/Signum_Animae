package com.example.chatservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PresenceHandshakeInterceptor presenceHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain STOMP-over-WebSocket endpoint (no SockJS fallback - keeps this
        // simple for a project of this size). Clients must connect DIRECTLY to
        // chat-service and now must include their own JWT, e.g.
        // ws://localhost:8083/ws-tattoo?userId=5&token=<jwt>
        // The gateway (port 8080) cannot proxy this - see the comment in
        // gateway-service/application.yaml. See PresenceHandshakeInterceptor for
        // why the token is required now.
        registry.addEndpoint("/ws-tattoo")
                .setAllowedOriginPatterns("*")
                .addInterceptors(presenceHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
