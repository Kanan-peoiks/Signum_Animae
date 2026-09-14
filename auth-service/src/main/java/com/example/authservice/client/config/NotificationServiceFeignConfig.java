package com.example.authservice.client.config;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/** booking-service-in AuthServiceFeignConfig-i ilə eyni quruluş: paylaşılan internal
 *  token hər sorğuya əlavə olunur.
 *
 *  Timeout-lar qəsdən qısadır: bu çağırış qeydiyyat/şifrə sıfırlama axınının içindən
 *  gedir, notification-service cavab verməsə istifadəçi dəqiqələrlə gözləməməlidir
 *  (çağıran tərəf xətanı onsuz da udur). */
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
