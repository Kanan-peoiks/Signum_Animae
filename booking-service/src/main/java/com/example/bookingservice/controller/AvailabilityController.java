package com.example.bookingservice.controller;

import com.example.bookingservice.dto.AvailabilitySlotRequest;
import com.example.bookingservice.dto.AvailabilitySlotResponse;
import com.example.bookingservice.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ustanın uyğunluq təqvimi. Mövcud sifariş yaratma axınına toxunmur - sırf
 *  məlumatlandırma xarakterlidir (bax AvailabilityService javadoc). */
@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<AvailabilitySlotResponse> addSlot(@Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.ok(availabilityService.addSlot(request));
    }

    /** Ustanın öz idarəetmə görünüşü - keçmiş/dolu daxil, hamısı. */
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<AvailabilitySlotResponse>> getSlotsForArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(availabilityService.getSlotsForArtist(artistId));
    }

    /** Müştəriyə göstərilən ictimai siyahı - yalnız gələcək və boş pəncərələr. */
    @GetMapping("/artist/{artistId}/public")
    public ResponseEntity<List<AvailabilitySlotResponse>> getPublicSlots(@PathVariable Long artistId) {
        return ResponseEntity.ok(availabilityService.getPublicSlots(artistId));
    }

    @PatchMapping("/{id}/booked")
    public ResponseEntity<AvailabilitySlotResponse> setBooked(@PathVariable Long id,
                                                               @RequestParam Long artistId,
                                                               @RequestParam boolean booked) {
        return ResponseEntity.ok(availabilityService.setBooked(id, artistId, booked));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id, @RequestParam Long artistId) {
        availabilityService.deleteSlot(id, artistId);
        return ResponseEntity.ok().build();
    }
}
