package com.example.chatservice.controller;

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

    /** Get-or-create: idempotent by bookingId, safe to call every time a chat screen
     *  opens. The caller must actually be one of the two parties named in the request -
     *  can't open (or "discover") a room pretending to be someone else. */
    @PostMapping
    public ResponseEntity<ChatRoomResponse> getOrCreateRoom(@Valid @RequestBody ChatRoomRequest request) {
        return ResponseEntity.ok(chatRoomService.getOrCreateRoom(request));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getRoom(roomId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsForCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(chatRoomService.getRoomsForCustomer(customerId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsForArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(chatRoomService.getRoomsForArtist(artistId));
    }

    /** Usta analitika paneli - göndərdiyi qiymət təkliflərinin qəbul/rədd nisbəti. */
    @GetMapping("/artist/{artistId}/offer-stats")
    public ResponseEntity<OfferStatsResponse> getOfferStats(@PathVariable Long artistId) {
        return ResponseEntity.ok(chatMessageService.getOfferStatsForArtist(artistId));
    }
}
