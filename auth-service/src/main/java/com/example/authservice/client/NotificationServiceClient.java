package com.example.authservice.client;

import com.example.authservice.client.config.NotificationServiceFeignConfig;
import com.example.authservice.client.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service",
             url = "${services.notification-service.url}",
             configuration = NotificationServiceFeignConfig.class)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send")
    void send(@RequestBody NotificationRequest request);
}
