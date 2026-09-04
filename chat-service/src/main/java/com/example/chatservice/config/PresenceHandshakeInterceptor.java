package com.example.chatservice.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * chat-service has no JWT wiring of its own for normal REST calls, and the gateway
 * CANNOT proxy a WebSocket upgrade at all (Spring Cloud Gateway Server MVC
 * limitation - see WebSocketConfig), so this endpoint is reached directly.
 *
 * Trusts the "?userId=" query param the client connects with (e.g.
 * ws://localhost:8083/ws-tattoo?userId=5) and attaches it to the WS session.
 */
@Component
public class PresenceHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, List<String>> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        String userId = firstOrNull(queryParams.get("userId"));
        if (userId != null && !userId.isBlank()) {
            attributes.put("userId", Long.valueOf(userId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String firstOrNull(List<String> values) {
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
