package com.example.chatservice.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Attaches the shared internal-service token to every call this service makes to
 *  booking-service's /internal/ endpoints (see booking-service's TrustedRequestFilter). */
@Configuration
public class BookingServiceFeignConfig {

    @Value("${internal.service-token:local-dev-internal-token}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Token", internalToken);
    }
}
