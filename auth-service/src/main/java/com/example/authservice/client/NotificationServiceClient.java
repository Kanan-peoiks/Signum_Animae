package com.example.authservice.client;

import com.example.authservice.client.config.NotificationServiceFeignConfig;
import com.example.authservice.client.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Gateway-i keçmədən birbaşa notification-service-ə (8085) - servislərarası çağırış,
 *  istifadəçinin JWT-si ilə gələn sorğu deyil. */
@FeignClient(name = "notification-service",
             url = "${services.notification-service.url}",
             configuration = NotificationServiceFeignConfig.class)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send")
    void send(@RequestBody NotificationRequest request);
}
