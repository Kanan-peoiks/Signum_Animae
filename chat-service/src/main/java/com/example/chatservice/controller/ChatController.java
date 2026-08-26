package com.example.chatservice.controller;

import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatMessageService.saveMessage(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @RequestParam Long user1,
            @RequestParam Long user2) {
        return ResponseEntity.ok(chatMessageService.getChatHistory(user1, user2));
    }
}