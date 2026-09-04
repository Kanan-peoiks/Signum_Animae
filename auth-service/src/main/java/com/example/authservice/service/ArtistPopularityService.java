package com.example.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Redis-backed artist popularity tracking (spec section 12):
 *   artist:view:{artistId}  - simple view counter (INCR)
 *   popular:artists         - sorted set, score = view count, used to rank artists
 *
 * Redis is NOT the source of truth (PostgreSQL is) - this is a best-effort ranking
 * signal only. If Redis is unreachable, callers must not fail because of it, which is
 * why every operation here swallows exceptions instead of propagating them.
 */
@Service
@RequiredArgsConstructor
public class ArtistPopularityService {

    private static final String VIEW_COUNTER_PREFIX = "artist:view:";
    private static final String POPULAR_ARTISTS_KEY = "popular:artists";

    private final StringRedisTemplate redisTemplate;

    public void recordView(Long artistProfileId) {
        try {
            redisTemplate.opsForValue().increment(VIEW_COUNTER_PREFIX + artistProfileId);
            redisTemplate.opsForZSet().incrementScore(POPULAR_ARTISTS_KEY, artistProfileId.toString(), 1);
        } catch (Exception ex) {
            // Popularity ranking is a nice-to-have; a Redis outage must never break
            // "view an artist profile".
        }
    }

    /** Usta analitika paneli üçün - sırf oxu, Redis əlçatmaz olsa 0 qaytarır (heç vaxt xəta atmır). */
    public long getViewCount(Long artistProfileId) {
        try {
            String value = redisTemplate.opsForValue().get(VIEW_COUNTER_PREFIX + artistProfileId);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception ex) {
            return 0L;
        }
    }

    public Set<Long> getPopularArtistIds(int limit) {
        try {
            Set<String> ids = redisTemplate.opsForZSet().reverseRange(POPULAR_ARTISTS_KEY, 0, limit - 1);
            if (ids == null || ids.isEmpty()) {
                return new LinkedHashSet<>();
            }
            Set<Long> result = new LinkedHashSet<>();
            for (String id : ids) {
                result.add(Long.valueOf(id));
            }
            return result;
        } catch (Exception ex) {
            return new LinkedHashSet<>();
        }
    }
}
