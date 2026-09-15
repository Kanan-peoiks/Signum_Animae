package com.example.bookingservice.controller;

import com.example.bookingservice.config.GatewayHeaders;
import com.example.bookingservice.dto.PageParams;
import com.example.bookingservice.dto.PageResponse;
import com.example.bookingservice.dto.ReviewReplyRequest;
import com.example.bookingservice.dto.ReviewRequest;
import com.example.bookingservice.dto.ReviewResponse;
import com.example.bookingservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(reviewService.createReview(request, callerId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsForArtist(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(PageResponse.from(
                reviewService.getReviewsForArtist(artistId, PageParams.of(page, size, sort))));
    }

    @PatchMapping("/{id}/reply")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(reviewService.addReply(id, request, callerId));
    }
}
