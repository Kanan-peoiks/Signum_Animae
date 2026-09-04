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
            @RequestParam(required = false) String minRating) {

        Double minRatingVal = (minRating != null && !minRating.isBlank()) ? Double.parseDouble(minRating) : null;
        return ResponseEntity.ok(artistService.searchArtists(city, style, minRatingVal));
    }

    @GetMapping("/public/popular")
    public ResponseEntity<List<ArtistProfileDto>> getPopularArtists(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(artistService.getPopularArtists(limit));
    }

    /**
     * {id} here is the artist's USER id (see ArtistService.getArtistByUserId javadoc) -
     * the same id returned as "userId" from /api/v1/auth/register and /login, and the
     * same id used as artistId in booking-service and chat-service.
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<ArtistProfileDto> getArtistById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.getArtistByUserId(id));
    }

    /**
     * Internal, service-to-service only (called by booking-service via Feign after a
     * review is created) - guarded by TrustedRequestFilter's "/internal/" rule (a shared
     * X-Internal-Token), not a user identity - there is no end-user token in a
     * server-to-server call.
     */
    /** Usta analitika paneli - profilin neçə dəfə baxıldığı. */
    @GetMapping("/{userId}/views")
    public ResponseEntity<Long> getViewCount(@PathVariable Long userId) {
        return ResponseEntity.ok(artistService.getViewCount(userId));
    }

    @PatchMapping("/internal/{artistId}/rating")
    public ResponseEntity<Void> updateRating(@PathVariable Long artistId, @Valid @RequestBody UpdateArtistRatingRequest request) {
        artistService.updateRatingAfterReview(artistId, request.getRating());
        return ResponseEntity.ok().build();
    }

    /**
     * Artist self-service profile edit (bio/experienceYears/styles). Requires the
     * verified caller (X-User-Id, set by the gateway from the JWT) to match {userId} -
     * an artist can only edit their own profile, not someone else's.
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<ArtistProfileDto> updateMyProfile(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateArtistProfileRequest request) {
        return ResponseEntity.ok(artistService.updateProfile(userId, request));
    }
}
