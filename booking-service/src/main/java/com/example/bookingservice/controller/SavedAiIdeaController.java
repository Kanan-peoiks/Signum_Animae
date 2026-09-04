package com.example.bookingservice.controller;

import com.example.bookingservice.dto.SaveAiIdeaRequest;
import com.example.bookingservice.dto.SavedAiIdeaResponse;
import com.example.bookingservice.service.SavedAiIdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** AI Studiyada alınmış ideyaların saxlanması/bəyənilməsi və mövcud sifarişə
 *  bağlanması. AI Studiyanın özünə (ai-service) toxunmur - sadəcə nəticəni
 *  saxlayır. */
@RestController
@RequestMapping("/api/v1/ai-ideas")
@RequiredArgsConstructor
public class SavedAiIdeaController {

    private final SavedAiIdeaService savedAiIdeaService;

    @PostMapping
    public ResponseEntity<SavedAiIdeaResponse> saveIdea(@Valid @RequestBody SaveAiIdeaRequest request) {
        return ResponseEntity.ok(savedAiIdeaService.saveIdea(request));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SavedAiIdeaResponse>> getSavedIdeas(@PathVariable Long customerId) {
        return ResponseEntity.ok(savedAiIdeaService.getSavedIdeas(customerId));
    }

    @PatchMapping("/{id}/link")
    public ResponseEntity<SavedAiIdeaResponse> linkToBooking(@PathVariable Long id,
                                                              @RequestParam Long customerId,
                                                              @RequestParam Long bookingId) {
        return ResponseEntity.ok(savedAiIdeaService.linkToBooking(id, customerId, bookingId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIdea(@PathVariable Long id, @RequestParam Long customerId) {
        savedAiIdeaService.deleteIdea(id, customerId);
        return ResponseEntity.ok().build();
    }
}
