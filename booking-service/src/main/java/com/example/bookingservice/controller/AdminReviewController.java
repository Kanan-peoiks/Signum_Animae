package com.example.bookingservice.controller;

import com.example.bookingservice.dto.PageParams;
import com.example.bookingservice.dto.PageResponse;
import com.example.bookingservice.dto.ReviewResponse;
import com.example.bookingservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "id");
        return ResponseEntity.ok(PageResponse.from(reviewService.getAllReviews(PageParams.of(page, size, newestFirst))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
