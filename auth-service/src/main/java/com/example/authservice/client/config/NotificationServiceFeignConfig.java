package com.example.authservice.client.config;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class NotificationServiceFeignConfig {

    @Value("${internal.service-token:local-dev-internal-token}")
    private String internalToken;

    @Bean
    public RequestInterceptor notificationInternalTokenInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Token", internalToken);
    }

    @Bean
    public Request.Options notificationRequestOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 4, TimeUnit.SECONDS, true);
    }
}
