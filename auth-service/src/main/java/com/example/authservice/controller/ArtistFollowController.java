package com.example.authservice.controller;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.dto.FollowRequest;
import com.example.authservice.service.ArtistFollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class ArtistFollowController {

    private final ArtistFollowService artistFollowService;

    @PostMapping
    public ResponseEntity<Void> follow(@Valid @RequestBody FollowRequest request) {
        artistFollowService.follow(request.getCustomerId(), request.getArtistId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unfollow(@RequestParam Long customerId, @RequestParam Long artistId) {
        artistFollowService.unfollow(customerId, artistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ArtistProfileDto>> followedArtists(@PathVariable Long customerId) {
        return ResponseEntity.ok(artistFollowService.followedArtists(customerId));
    }

    @GetMapping("/artist/{artistId}/count")
    public ResponseEntity<Long> followerCount(@PathVariable Long artistId) {
        return ResponseEntity.ok(artistFollowService.followerCount(artistId));
    }

    /** Usta profil səhifəsi düymənin hansı vəziyyətdə çıxacağını bilməlidir -
     *  bunun üçün bütün izləmə siyahısını çəkmək əvəzinə tək sorğu. */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> isFollowing(@RequestParam Long customerId, @RequestParam Long artistId) {
        return ResponseEntity.ok(artistFollowService.isFollowing(customerId, artistId));
    }
}
