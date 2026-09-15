package com.example.bookingservice.controller;

import com.example.bookingservice.config.GatewayHeaders;
import com.example.bookingservice.dto.SaveAiIdeaRequest;
import com.example.bookingservice.dto.SavedAiIdeaResponse;
import com.example.bookingservice.security.AccessGuard;
import com.example.bookingservice.service.SavedAiIdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-ideas")
@RequiredArgsConstructor
public class SavedAiIdeaController {

    private final SavedAiIdeaService savedAiIdeaService;

    @PostMapping
    public ResponseEntity<SavedAiIdeaResponse> saveIdea(
            @Valid @RequestBody SaveAiIdeaRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(savedAiIdeaService.saveIdea(request, callerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SavedAiIdeaResponse>> getSavedIdeas(
            @PathVariable Long customerId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(customerId, callerId);
        return ResponseEntity.ok(savedAiIdeaService.getSavedIdeas(customerId));
    }

    @PatchMapping("/{id}/link")
    public ResponseEntity<SavedAiIdeaResponse> linkToBooking(
            @PathVariable Long id,
            @RequestParam Long bookingId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(savedAiIdeaService.linkToBooking(id, callerId, bookingId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIdea(
            @PathVariable Long id,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        savedAiIdeaService.deleteIdea(id, callerId);
        return ResponseEntity.ok().build();
    }
}
