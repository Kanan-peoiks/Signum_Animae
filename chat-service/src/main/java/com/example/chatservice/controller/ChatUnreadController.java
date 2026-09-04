package com.example.chatservice.controller;

import com.example.chatservice.dto.UnreadCountResponse;
import com.example.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Separate, tiny controller for the one cross-room aggregate the frontend needs (the
 * "Söhbətlər" nav badge) - didn't fit ChatController (/rooms/{roomId}/messages) or
 * ChatRoomController (/rooms/**) without stretching either one's path shape.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatUnreadController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(new UnreadCountResponse(chatMessageService.getUnreadCountForUser(userId)));
    }
}
