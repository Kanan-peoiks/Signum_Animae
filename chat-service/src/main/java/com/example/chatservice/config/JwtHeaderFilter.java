package com.example.chatservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * chat-service tək servisdir ki, internetə birbaşa açıq qalmalıdır - gateway
 * WebSocket "upgrade" əməliyyatını proxy edə bilmir, ona görə klient 8083-ə özü
 * qoşulur. Bu isə o deməkdir ki, servisin REST endpoint-lərinə də gateway-i
 * keçmədən vurmaq mümkündür.
 *
 * Digər servislərdə X-User-Id başlığına etibar etmək kifayətdir, çünki onlara
 * yalnız daxili şəbəkədən çatmaq olur. Burada isə kifayət etmir: kənar biri
 * başlığı özü yazıb istənilən istifadəçi kimi görünə bilərdi.
 *
 * Ona görə bu filtr:
 *   1. Klientdən gələn X-User-Id başlığını HƏMİŞƏ silir;
 *   2. Authorization: Bearer <jwt> etibarlıdırsa, başlığı tokenin subject-indən
 *      YENİDƏN yazır.
 *
 * Nəticədə başlıq yalnız doğrulanmış tokendən gələ bilər - istər gateway
 * üzərindən gəlsin, istər birbaşa. Token yoxdursa başlıq da olmur və
 * AccessGuard/requireParticipant 403 qaytarır.
 */
@Component
@RequiredArgsConstructor
public class JwtHeaderFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/ws-tattoo");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Long verifiedUserId = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            verifiedUserId = jwtUtil.extractUserId(authHeader.substring(7));
        }
        filterChain.doFilter(new VerifiedUserRequest(request, verifiedUserId), response);
    }

    /** X-User-Id başlığını klientdən gizlədir və yalnız doğrulanmış dəyəri göstərir. */
    private static class VerifiedUserRequest extends HttpServletRequestWrapper {

        private final String verifiedUserId;

        VerifiedUserRequest(HttpServletRequest request, Long verifiedUserId) {
            super(request);
            this.verifiedUserId = verifiedUserId == null ? null : String.valueOf(verifiedUserId);
        }

        private boolean isGuarded(String name) {
            return GatewayHeaders.USER_ID.equalsIgnoreCase(name);
        }

        @Override
        public String getHeader(String name) {
            if (isGuarded(name)) {
                return verifiedUserId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isGuarded(name)) {
                return verifiedUserId == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(verifiedUserId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new java.util.ArrayList<>(Collections.list(super.getHeaderNames()));
            names.removeIf(this::isGuarded);
            if (verifiedUserId != null) {
                names.add(GatewayHeaders.USER_ID);
            }
            return Collections.enumeration(names);
        }
    }
}
