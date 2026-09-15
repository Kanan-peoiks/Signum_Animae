package com.example.gatewayservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * "Kim olduğun bilinmir" ilə "kimsən bilirik, amma buna icazən yoxdur" hallarını
 * AYIRIR.
 *
 * Əvvəl hər iki hal 403 qaytarırdı (Spring Security-nin stateless konfiqurasiyada
 * standart davranışı), frontend isə hər 403-ü "sessiya bitdi" sayıb istifadəçini
 * çıxarırdı. Bu, indi real problemdir: chat-service-in "bu söhbət sizə aid deyil"
 * cavabı da 403-dür - yəni bir yanlış klikdən sonra istifadəçi tamamilə çıxarılardı.
 *
 * İndi:
 *   401 - token yoxdur / etibarsızdır / vaxtı bitib  → frontend giriş ekranına qaytarır
 *   403 - token etibarlıdır, amma icazə çatmır       → frontend sadəcə xətanı göstərir
 *
 * Downstream servislərin öz 403-ləri (məs. NotRoomParticipantException) buradan
 * keçmir - onlar proxy ilə olduğu kimi ötürülür, öz mesajları ilə birlikdə.
 */
@Configuration
public class AuthErrorConfig {

    @Bean
    public AuthenticationEntryPoint unauthenticatedEntryPoint() {
        return (request, response, authException) ->
                writeJson(response, HttpStatus.UNAUTHORIZED,
                        "Sessiya bitib və ya token yoxdur. Yenidən daxil ol.");
    }

    @Bean
    public AccessDeniedHandler forbiddenHandler() {
        return (request, response, accessDeniedException) ->
                writeJson(response, HttpStatus.FORBIDDEN,
                        "Bu əməliyyat üçün icazən yoxdur.");
    }

    /** Servislərin GlobalExceptionHandler-lərinin qaytardığı ErrorResponse ilə eyni
     *  forma - frontend hər yerdə "message" sahəsini oxuyur. */
    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"message\":\"" + message + "\","
                        + "\"status\":" + status.value() + ","
                        + "\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }
}
