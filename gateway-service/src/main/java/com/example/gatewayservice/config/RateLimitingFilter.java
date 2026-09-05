package com.example.gatewayservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deliberately simple, deliberately generous rate limiting on ONLY the login and
 * register endpoints - the two paths someone would hammer to brute-force a password
 * or spam fake accounts. Nothing else in the system is touched by this filter.
 *
 * Fixed-window counter keyed by client IP, held in memory (no Redis needed here - a
 * single gateway instance is plenty at this project's scale). If anything about this
 * filter itself misbehaves (an unexpected exception, whatever), it fails OPEN: the
 * request is let through rather than risking every login getting blocked because of a
 * bug in the limiter - this touches the login path, so a mistake here must never be
 * worse than not rate-limiting at all.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String[] LIMITED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    };

    /** Generous on purpose - this only needs to stop scripted abuse, not slow down a
     *  real person who mistypes their password a few times in a row. */
    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000L; // 1 dəqiqə
    private static final long STALE_AFTER_MILLIS = WINDOW_MILLIS * 5;
    private static final int PRUNE_THRESHOLD = 5000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (isLimitedPath(request) && isOverLimit(clientIp(request))) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"message\":\"Çox tez-tez cəhd edildi, bir azdan yenidən yoxla.\"}");
                return;
            }
        } catch (Exception ex) {
            // Fail open: a bug in the limiter must never block a real login/register.
            logger.warn("RateLimitingFilter uğursuz oldu, sorğu buraxılır", ex);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLimitedPath(HttpServletRequest request) {
        // Yalnız POST - CORS preflight (OPTIONS) onsuz da CorsFilter tərəfindən
        // zəncirin bu nöqtəsinə çatmadan tutulur, amma dəqiqlik üçün qeyd edək.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        for (String limited : LIMITED_PATHS) {
            if (path.equals(limited)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverLimit(String key) {
        long now = System.currentTimeMillis();
        pruneOccasionally(now);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart > WINDOW_MILLIS) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            bucket.count++;
            return bucket.count > MAX_REQUESTS_PER_WINDOW;
        }
    }

    /** Every request checking the whole map would defeat the purpose of a lightweight
     *  filter, so cleanup only kicks in occasionally, once the map has grown large
     *  enough that leaving stale IPs in it would actually matter. */
    private void pruneOccasionally(long now) {
        if (buckets.size() < PRUNE_THRESHOLD || Math.random() > 0.01) {
            return;
        }
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > STALE_AFTER_MILLIS);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Bucket {
        volatile long windowStart;
        int count;

        Bucket(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
