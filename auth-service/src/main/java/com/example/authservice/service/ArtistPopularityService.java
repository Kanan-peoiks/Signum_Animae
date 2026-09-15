package com.example.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

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
        }
    }

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
