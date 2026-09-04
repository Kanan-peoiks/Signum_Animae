package com.example.notificationservice.client;

import com.example.notificationservice.client.config.AuthServiceFeignConfig;
import com.example.notificationservice.client.dto.InternalUserContactDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Resolves a user's real email server-side instead of trusting whatever address the
 * frontend (or a caller impersonating the frontend) sent along with a notification
 * request - see NotificationService.sendNotification.
 */
@FeignClient(name = "auth-service", url = "${services.auth-service.url}", configuration = AuthServiceFeignConfig.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/internal/{id}/contact")
    InternalUserContactDto getUserContact(@PathVariable("id") Long id);
}
