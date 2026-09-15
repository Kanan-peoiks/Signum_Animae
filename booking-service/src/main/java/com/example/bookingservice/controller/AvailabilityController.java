package com.example.bookingservice.controller;

import com.example.bookingservice.config.GatewayHeaders;
import com.example.bookingservice.dto.AvailabilitySlotRequest;
import com.example.bookingservice.dto.AvailabilitySlotResponse;
import com.example.bookingservice.security.AccessGuard;
import com.example.bookingservice.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<AvailabilitySlotResponse> addSlot(
            @Valid @RequestBody AvailabilitySlotRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(availabilityService.addSlot(request, callerId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<AvailabilitySlotResponse>> getSlotsForArtist(
            @PathVariable Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(artistId, callerId);
        return ResponseEntity.ok(availabilityService.getSlotsForArtist(artistId));
    }

    @GetMapping("/artist/{artistId}/public")
    public ResponseEntity<List<AvailabilitySlotResponse>> getPublicSlots(@PathVariable Long artistId) {
        return ResponseEntity.ok(availabilityService.getPublicSlots(artistId));
    }

    @PatchMapping("/{id}/booked")
    public ResponseEntity<AvailabilitySlotResponse> setBooked(
            @PathVariable Long id,
            @RequestParam boolean booked,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(availabilityService.setBooked(id, callerId, booked));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long id,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        availabilityService.deleteSlot(id, callerId);
        return ResponseEntity.ok().build();
    }
}
