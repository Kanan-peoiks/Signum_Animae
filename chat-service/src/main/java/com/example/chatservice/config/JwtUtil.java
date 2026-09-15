package com.example.chatservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * WebSocket handshake-i üçün minimal JWT yoxlayıcısı.
 *
 * REST sorğular gateway-dən keçir və kimlik oradan X-User-Id başlığı ilə gəlir, ona görə
 * bu sinif YALNIZ WebSocket üçün lazımdır: gateway WebSocket "upgrade"-ini proxy edə
 * bilmir (bax gateway-service/application.yaml), yəni klient birbaşa 8083-ə qoşulur və
 * burada onu yoxlayan başqa heç nə yoxdur.
 *
 * Eyni secret-dən istifadə edir (gateway və auth-service ilə) - token auth-service-də
 * verilir, subject isə istifadəçinin rəqəmsal id-sidir.
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** @return tokendəki istifadəçi id-si, token etibarsızdırsa/oxunmursa null. */
    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String subject = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getSubject();
            return subject == null ? null : Long.valueOf(subject);
        } catch (Exception ex) {
            // İmza səhvdir, vaxtı bitib və ya subject rəqəm deyil - hər halda etibarsızdır.
            return null;
        }
    }
}
