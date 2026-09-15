package com.example.chatservice.controller;

import com.example.chatservice.config.GatewayHeaders;
import com.example.chatservice.dto.ChatRoomRequest;
import com.example.chatservice.dto.ChatRoomResponse;
import com.example.chatservice.dto.OfferStatsResponse;
import com.example.chatservice.service.ChatMessageService;
import com.example.chatservice.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> getOrCreateRoom(
            @Valid @RequestBody ChatRoomRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatRoomService.getOrCreateRoom(request, callerId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomResponse> getRoom(
            @PathVariable Long roomId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatRoomService.getRoom(roomId, callerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsForCustomer(
            @PathVariable Long customerId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatRoomService.getRoomsForCustomer(customerId, callerId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsForArtist(
            @PathVariable Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(chatRoomService.getRoomsForArtist(artistId, callerId));
    }

    @GetMapping("/artist/{artistId}/offer-stats")
    public ResponseEntity<OfferStatsResponse> getOfferStats(
            @PathVariable Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        chatRoomService.requireSelf(artistId, callerId);
        return ResponseEntity.ok(chatMessageService.getOfferStatsForArtist(artistId));
    }
}
