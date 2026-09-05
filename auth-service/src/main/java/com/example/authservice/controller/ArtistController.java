package com.example.authservice.controller;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.dto.UpdateArtistProfileRequest;
import com.example.authservice.dto.UpdateArtistRatingRequest;
import com.example.authservice.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping("/public/search")
    public ResponseEntity<List<ArtistProfileDto>> searchArtists(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String minRating,
            @RequestParam(required = false) String minExperience,
            @RequestParam(required = false) String sortBy) {

        Double minRatingVal = (minRating != null && !minRating.isBlank()) ? Double.parseDouble(minRating) : null;
        Integer minExperienceVal = (minExperience != null && !minExperience.isBlank()) ? Integer.parseInt(minExperience) : null;
        return ResponseEntity.ok(artistService.searchArtists(city, style, minRatingVal, minExperienceVal, sortBy));
    }

    @GetMapping("/public/popular")
    public ResponseEntity<List<ArtistProfileDto>> getPopularArtists(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(artistService.getPopularArtists(limit));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ArtistProfileDto> getArtistById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.getArtistByUserId(id));
    }


    // Usta analitika paneli
    @GetMapping("/{userId}/views")
    public ResponseEntity<Long> getViewCount(@PathVariable Long userId) {
        return ResponseEntity.ok(artistService.getViewCount(userId));
    }

    @PatchMapping("/internal/{artistId}/rating")
    public ResponseEntity<Void> updateRating(@PathVariable Long artistId, @Valid @RequestBody UpdateArtistRatingRequest request) {
        artistService.updateRatingAfterReview(artistId, request.getRating());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ArtistProfileDto> updateMyProfile(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateArtistProfileRequest request) {
        return ResponseEntity.ok(artistService.updateProfile(userId, request));
    }
}
