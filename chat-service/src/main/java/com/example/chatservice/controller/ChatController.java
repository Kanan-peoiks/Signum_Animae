package com.example.chatservice.controller;

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

/**
 * Plain REST fallback for chat, for clients that are not using the
 * WebSocket/STOMP connection (e.g. quick Postman testing). Sending through
 * here still broadcasts to any WebSocket subscribers of the room, since it
 * shares ChatMessageService.saveMessage() with the STOMP controller.
 *
 * Çağıranın kimliyi HƏMİŞƏ X-User-Id başlığından götürülür. Bu başlığı yalnız
 * gateway qoya bilər: JwtAuthenticationFilter klientdən gələn eyni adlı başlığı
 * əvvəlcə silir və yalnız JWT doğrulandıqdan sonra öz dəyərini yazır. Sorğunun
 * gövdəsindəki senderId/userId sahələrinə etibar edilmir - əks halda istənilən
 * istifadəçi özünü başqası kimi təqdim edə bilərdi.
 *
 * Başlıq yoxdursa (məsələn kimsə gateway-i keçib birbaşa 8083-ə vurur) kimlik
 * naməlum qalır və ChatMessageService.requireParticipant 403 qaytarır.
 */
@RestController
@RequestMapping("/api/v1/chat/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatController {

    /** Gateway-in doğruladığı istifadəçi id-si - bax JwtAuthenticationFilter. */
    private static final String CALLER_HEADER = "X-User-Id";

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request,
            @RequestHeader(value = CALLER_HEADER, required = false) Long callerId) {
        return ResponseEntity.ok(chatMessageService.saveMessage(roomId, request, callerId));
    }

    /**
     * Səhifələnmiş tarixçə, TƏRS istiqamətdə: {@code page=0} ən YENİ mesajlardır,
     * {@code page=1} ondan əvvəlkilər və s. ("Köhnə mesajları yüklə" məntəqi).
     *
     * Səhifənin İÇİNDKİ sıra isə normaldır - köhnədən yeniyə, ekranda olduğu kimi:
     * verilənlər bazasından DESC gələn səhifəni burada çeviririk ki, klient əlavə iş
     * görmədən "content"-i olduğu kimi yazışma sahəsinə əlavə edə bilsin.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ChatMessageResponse>> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestHeader(value = CALLER_HEADER, required = false) Long callerId) {

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
            @RequestHeader(value = CALLER_HEADER, required = false) Long callerId) {
        chatMessageService.markAsRead(roomId, callerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Customer accepts or rejects an artist's OFFER message. On accept, this
     * calls booking-service to actually update the booking's price (see
     * ChatMessageService.respondToOffer) and posts a SYSTEM message into the
     * room so both sides see the outcome.
     */
    @PatchMapping("/{messageId}/offer")
    public ResponseEntity<ChatMessageResponse> respondToOffer(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody OfferResponseRequest request,
            @RequestHeader(value = CALLER_HEADER, required = false) Long callerId) {
        return ResponseEntity.ok(chatMessageService.respondToOffer(roomId, messageId, request.isAccept(), callerId));
    }
}
