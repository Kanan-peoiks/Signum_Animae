package com.example.chatservice.controller;

import com.example.chatservice.config.GatewayHeaders;
import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.dto.OfferResponseRequest;
import com.example.chatservice.dto.ChatMessageResponse;
import com.example.chatservice.dto.PageParams;
import com.example.chatservice.dto.PageResponse;
import com.example.chatservice.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatMessageService.saveMessage(roomId, request, callerId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ChatMessageResponse>> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<ChatMessageResponse> newestPage =
                chatMessageService.getHistory(roomId, callerId, PageParams.of(page, size, newestFirst));

        PageResponse<ChatMessageResponse> body = PageResponse.from(newestPage);
        List<ChatMessageResponse> chronological = new ArrayList<>(body.getContent());
        Collections.reverse(chronological);
        body.setContent(chronological);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        chatMessageService.markAsRead(roomId, callerId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{messageId}/offer")
    public ResponseEntity<ChatMessageResponse> respondToOffer(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody OfferResponseRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatMessageService.respondToOffer(roomId, messageId, request.isAccept(), callerId));
    }
}
