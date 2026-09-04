package com.example.chatservice.controller;

import com.example.chatservice.service.PresenceTypingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceTypingService presenceTypingService;

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> isOnline(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "online", presenceTypingService.isOnline(userId)
        ));
    }
}
