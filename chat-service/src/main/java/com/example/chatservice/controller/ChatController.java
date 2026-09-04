package com.example.chatservice.controller;

import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.dto.OfferResponseRequest;
import com.example.chatservice.dto.ChatMessageResponse;
import com.example.chatservice.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plain REST fallback for chat, for clients that are not using the
 * WebSocket/STOMP connection (e.g. quick Postman testing). Sending through
 * here still broadcasts to any WebSocket subscribers of the room, since it
 * shares ChatMessageService.saveMessage() with the STOMP controller.
 */
@RestController
@RequestMapping("/api/v1/chat/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(@PathVariable Long roomId,
                                                            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatMessageService.saveMessage(roomId, request, request.getSenderId()));
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatMessageService.getHistory(roomId, null));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long roomId, @RequestParam Long userId) {
        chatMessageService.markAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Customer accepts or rejects an artist's OFFER message. On accept, this
     * calls booking-service to actually update the booking's price (see
     * ChatMessageService.respondToOffer) and posts a SYSTEM message into the
     * room so both sides see the outcome.
     */
    @PatchMapping("/{messageId}/offer")
    public ResponseEntity<ChatMessageResponse> respondToOffer(@PathVariable Long roomId,
                                                               @PathVariable Long messageId,
                                                               @Valid @RequestBody OfferResponseRequest request) {
        return ResponseEntity.ok(chatMessageService.respondToOffer(roomId, messageId, request.isAccept(), request.getUserId()));
    }
}
