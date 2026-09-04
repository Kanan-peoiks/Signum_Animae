package com.example.chatservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Presence and typing-indicator state is "nice to have" ephemeral data, so
 * every call is wrapped so a Redis outage never breaks the actual chat.
 */
@Service
@RequiredArgsConstructor
public class PresenceTypingService {

    private static final String ONLINE_PREFIX = "online:user:";
    private static final String TYPING_PREFIX_TEMPLATE = "chat:%s:typing:%s";

    private final StringRedisTemplate redisTemplate;

    public void markOnline(Long userId) {
        try {
            redisTemplate.opsForValue().set(ONLINE_PREFIX + userId, "true", Duration.ofMinutes(2));
        } catch (Exception ignored) {
        }
    }

    public void markOffline(Long userId) {
        try {
            redisTemplate.delete(ONLINE_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }

    public boolean isOnline(Long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_PREFIX + userId));
        } catch (Exception ignored) {
            return false;
        }
    }

    public void markTyping(Long roomId, Long userId) {
        try {
            String key = String.format(TYPING_PREFIX_TEMPLATE, roomId, userId);
            redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(5));
        } catch (Exception ignored) {
        }
    }
}
