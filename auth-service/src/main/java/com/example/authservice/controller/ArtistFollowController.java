package com.example.authservice.controller;

import com.example.authservice.config.GatewayHeaders;
import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.dto.FollowRequest;
import com.example.authservice.security.AccessGuard;
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
    public ResponseEntity<Void> follow(
            @Valid @RequestBody FollowRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        artistFollowService.follow(callerId, request.getArtistId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unfollow(
            @RequestParam Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        artistFollowService.unfollow(callerId, artistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ArtistProfileDto>> followedArtists(
            @PathVariable Long customerId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(customerId, callerId);
        return ResponseEntity.ok(artistFollowService.followedArtists(customerId));
    }

    @GetMapping("/artist/{artistId}/count")
    public ResponseEntity<Long> followerCount(@PathVariable Long artistId) {
        return ResponseEntity.ok(artistFollowService.followerCount(artistId));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> isFollowing(
            @RequestParam Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(artistFollowService.isFollowing(callerId, artistId));
    }
}
