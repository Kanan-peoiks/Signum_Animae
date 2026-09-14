package com.example.authservice.service;

import com.example.authservice.client.NotificationServiceClient;
import com.example.authservice.client.dto.NotificationRequest;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.model.AuthToken;
import com.example.authservice.model.AuthTokenType;
import com.example.authservice.repo.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Şifrə sıfırlama və email təsdiqi tokenlərinin verilməsi, yoxlanması və
 *  müvafiq məktubun notification-service üzərindən göndərilməsi. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    /** Şifrə sıfırlama linki qısa yaşamalıdır - ələ keçsə də tez yararsız olsun. */
    private static final long PASSWORD_RESET_HOURS = 1;
    /** Təsdiq linki üçün bir saat çox azdır (məktub gec oxuna bilər). */
    private static final long EMAIL_VERIFICATION_HOURS = 24;

    private final AuthTokenRepository authTokenRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Value("${app.frontend-url:http://localhost:5500}")
    private String frontendUrl;

    /** Eyni məqsədlə əvvəlki istifadə olunmamış tokenləri ləğv edir və yenisini verir -
     *  belədə bir anda yalnız bir etibarlı link olur. */
    @Transactional
    public String issue(Long userId, AuthTokenType type) {
        List<AuthToken> previous = authTokenRepository.findByUserIdAndTypeAndUsedFalse(userId, type);
        previous.forEach(t -> t.setUsed(true));
        if (!previous.isEmpty()) {
            authTokenRepository.saveAll(previous);
        }

        long hours = type == AuthTokenType.PASSWORD_RESET ? PASSWORD_RESET_HOURS : EMAIL_VERIFICATION_HOURS;
        AuthToken token = AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(hours))
                .used(false)
                .build();

        return authTokenRepository.save(token).getToken();
    }

    /** Tokeni yoxlayır və dərhal "istifadə olunub" işarələyir (bir dəfəlikdir).
     *  Vaxtı bitib, artıq istifadə olunub və ya ümumiyyətlə yoxdursa - eyni ümumi
     *  mesajla 400: hansı halda olduğunu bilmək hücum edənə əlavə məlumat verir. */
    @Transactional
    public AuthToken consume(String rawToken, AuthTokenType expectedType) {
        AuthToken token = authTokenRepository.findByToken(rawToken)
                .filter(t -> t.getType() == expectedType)
                .filter(AuthToken::isUsable)
                .orElseThrow(() -> new InvalidTokenException(
                        "Link etibarsızdır və ya vaxtı bitib. Zəhmət olmasa yenisini istə."));

        token.setUsed(true);
        return authTokenRepository.save(token);
    }

    public void sendPasswordResetEmail(Long userId, String token) {
        String link = frontendUrl + "/index.html?mode=reset&token=" + token;
        sendQuietly(userId, "Şifrənin bərpası",
                "Şifrəni yeniləmək üçün bu linkə keç:\n\n" + link +
                "\n\nLink 1 saat ərzində etibarlıdır. Əgər bu sorğunu sən göndərməmisənsə, " +
                "bu məktubu nəzərə alma - şifrən dəyişməyib.");
    }

    public void sendVerificationEmail(Long userId, String token) {
        String link = frontendUrl + "/index.html?mode=verify&token=" + token;
        sendQuietly(userId, "Email ünvanını təsdiqlə",
                "SIGNUM ANIMAE-yə xoş gəldin! Email ünvanını təsdiqləmək üçün bu linkə keç:\n\n" + link +
                "\n\nLink 24 saat ərzində etibarlıdır.");
    }

    /** Məktub göndərilməsə də əsas əməliyyat (qeydiyyat, şifrə sıfırlama sorğusu)
     *  sınmamalıdır - xəta yalnız log-a düşür. */
    private void sendQuietly(Long userId, String title, String message) {
        try {
            notificationServiceClient.send(NotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .sendEmail(true)
                    .build());
        } catch (Exception ex) {
            log.error("Bildiriş göndərilmədi (userId={}, title={}): {}", userId, title, ex.getMessage(), ex);
        }
    }
}
