package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Comma-separated list, e.g. "https://app.example.com,https://admin.example.com".
     *  Defaults to "*" so nothing breaks today - narrow this to the frontend's real
     *  origin(s) via the CORS_ALLOWED_ORIGINS env var before this goes anywhere public;
     *  a wildcard origin on a gateway that accepts an Authorization header is fine for
     *  local dev only. */
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                // Only the verbs this API actually exposes - previously "*", which also
                // allowed PUT/TRACE/etc. that no endpoint here uses. DELETE added for the
                // availability-slot cleanup endpoint (booking-service).
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                // Only what a request here actually needs, instead of "*".
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}
