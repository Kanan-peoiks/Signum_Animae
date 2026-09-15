package com.example.chatservice.controller;

import com.example.chatservice.config.GatewayHeaders;
import com.example.chatservice.dto.UnreadCountResponse;
import com.example.chatservice.service.ChatMessageService;
import com.example.chatservice.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatUnreadController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable Long userId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        chatRoomService.requireSelf(userId, callerId);
        return ResponseEntity.ok(new UnreadCountResponse(chatMessageService.getUnreadCountForUser(userId)));
    }
}
