package com.example.bookingservice.client;

import com.example.bookingservice.client.config.NotificationServiceFeignConfig;
import com.example.bookingservice.client.dto.NotificationRequest;
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
