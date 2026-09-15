package com.example.chatservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String subject = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getSubject();
            return subject == null ? null : Long.valueOf(subject);
        } catch (Exception ex) {
            return null;
        }
    }
}
