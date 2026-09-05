package com.example.gatewayservice.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Beyond authenticating the request at the gateway, this filter is also the ONLY
 * place in the whole system that is allowed to say "this request is really from
 * user X" to the downstream services - none of them (booking/chat/ai/notification)
 * have their own JWT/Spring Security setup, they trust the X-User-Id/X-User-Role
 * headers set here (see each service's TrustedRequestFilter).
 *
 * That trust is only meaningful if a client can never set those headers itself, so
 * this filter ALWAYS strips any incoming X-User-Id/X-User-Role first (whether or
 * not the token is valid), and only re-adds them once the JWT has actually been
 * verified. What this does NOT protect against: someone bypassing the gateway
 * entirely and hitting booking-service/chat-service's own port directly on the
 * network, forging the header there themselves - that needs real network
 * isolation (Docker network / firewall), which is out of scope here for now.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final JwtUtil jwtUtil;

    private static final String[] PUBLIC_PREFIXES = {
            "/api/v1/auth/",
            "/api/v1/artists/public/",
            "/ws-tattoo/"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Defense in depth: never let a client-supplied value through, even on public
        // paths or when auth fails below - downstream services should never see a
        // X-User-Id/X-User-Role that didn't come from a verified token right here.
        TrustedHeaderRequestWrapper wrapped = new TrustedHeaderRequestWrapper(request);

        String path = request.getRequestURI();
        for (String prefix : PUBLIC_PREFIXES) {
            if (path != null && path.startsWith(prefix)) {
                filterChain.doFilter(wrapped, response);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(wrapped, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(wrapped, response);
            return;
        }

        String userId = jwtUtil.getUsername(token); // numeric user id, despite the method name
        String role = jwtUtil.getRole(token);
        wrapped.setTrustedHeader(HEADER_USER_ID, userId);
        wrapped.setTrustedHeader(HEADER_USER_ROLE, role);

        // hasRole("X") in SecurityConfig needs an authority literally named "ROLE_X" - this
        // is additive only: every existing route only checks .anyRequest().authenticated(),
        // which doesn't look at authorities at all, so populating them here can't change
        // behaviour for any route other than the new /api/v1/admin/** one.
        java.util.List<GrantedAuthority> authorities = (role != null && !role.isBlank())
                ? java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : Collections.emptyList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(wrapped, response);
    }

    /** Strips X-User-Id/X-User-Role from whatever the client sent, and optionally
     *  overrides them with a verified value from setTrustedHeader(). */
    private static class TrustedHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> trustedHeaders = new HashMap<>();

        TrustedHeaderRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        void setTrustedHeader(String name, String value) {
            if (value != null) {
                trustedHeaders.put(name.toLowerCase(), value);
            }
        }

        private boolean isGuarded(String name) {
            return name != null &&
                    (name.equalsIgnoreCase(HEADER_USER_ID) || name.equalsIgnoreCase(HEADER_USER_ROLE));
        }

        @Override
        public String getHeader(String name) {
            if (isGuarded(name)) {
                return trustedHeaders.get(name.toLowerCase());
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isGuarded(name)) {
                String value = trustedHeaders.get(name.toLowerCase());
                return value == null ? Collections.emptyEnumeration() : Collections.enumeration(java.util.List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.List<String> names = Collections.list(super.getHeaderNames());
            names.removeIf(this::isGuarded);
            names.addAll(trustedHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
