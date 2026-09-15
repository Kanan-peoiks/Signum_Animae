package com.example.notificationservice.controller;

import com.example.notificationservice.config.GatewayHeaders;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.PageParams;
import com.example.notificationservice.dto.PageResponse;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.exception.NotOwnerException;
import com.example.notificationservice.security.AccessGuard;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${internal.service-token:local-dev-internal-token}")
    private String internalToken;

    @PostMapping("/send")
    public ResponseEntity<Notification> sendNotification(
            @Valid @RequestBody NotificationRequest request,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new NotOwnerException("Bildiriş göndərmək yalnız servislərarası çağırışlara icazəlidir.");
        }
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<Notification>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(userId, callerId);

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(PageResponse.from(
                notificationService.getUserNotifications(userId, PageParams.of(page, size, newestFirst))));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long userId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(userId, callerId);
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        notificationService.markAsRead(id, callerId);
        return ResponseEntity.ok().build();
    }
}
