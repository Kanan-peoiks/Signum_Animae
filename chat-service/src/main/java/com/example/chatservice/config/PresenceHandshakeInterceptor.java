package com.example.chatservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * chat-service-in REST tərəfində öz JWT quraşdırması yoxdur (kimlik gateway-in
 * X-User-Id başlığından gəlir), gateway isə WebSocket "upgrade"-ini ümumiyyətlə proxy
 * edə bilmir (Spring Cloud Gateway Server MVC məhdudiyyəti - bax WebSocketConfig).
 * Yəni bu endpoint birbaşa açıqdır və onu qoruyan yeganə yer buradır.
 *
 * ƏVVƏL: sadəcə "?userId=" parametrinə inanılırdı - yəni istənilən kəs
 * ws://localhost:8083/ws-tattoo?userId=16 ilə qoşulub özünü başqası kimi təqdim edə,
 * onun söhbətlərinə mesaj yaza bilərdi.
 * İNDİ: "?token=" mütləqdir və imzası yoxlanılır; sessiyaya yazılan userId tokenin
 * subject-indən götürülür, klientin dediyi "userId" nəzərə alınmır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, List<String>> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        Long userId = jwtUtil.extractUserId(firstOrNull(queryParams.get("token")));
        if (userId == null) {
            // Kimliyi yoxlanmayan qoşulmanı ümumiyyətlə qəbul etmirik - "userId yazılmadan
            // davam et" varianti otaq iştirakçısı yoxlamasını mənasız edərdi.
            log.warn("WebSocket handshake rədd edildi: token yoxdur və ya etibarsızdır.");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", userId);
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
