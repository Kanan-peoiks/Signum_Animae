package com.example.bookingservice.controller;

import com.example.bookingservice.dto.ReviewRequest;
import com.example.bookingservice.dto.ReviewResponse;
import com.example.bookingservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request, request.getCustomerId()));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(reviewService.getReviewsForArtist(artistId));
    }
}
