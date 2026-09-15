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
