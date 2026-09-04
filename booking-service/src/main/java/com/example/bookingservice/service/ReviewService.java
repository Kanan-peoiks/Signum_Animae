package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.dto.UpdateArtistRatingRequest;
import com.example.bookingservice.dto.ReviewReplyRequest;
import com.example.bookingservice.dto.ReviewRequest;
import com.example.bookingservice.dto.ReviewResponse;
import com.example.bookingservice.exception.BookingNotCompletedException;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.exception.ReviewAlreadyExistsException;
import com.example.bookingservice.exception.ReviewNotFoundException;
import com.example.bookingservice.exception.ReviewOwnershipException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.model.Review;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long callerId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Bron tapılmadı! ID: " + request.getBookingId()));

        // Ownership is checked against the VERIFIED caller (X-User-Id from the gateway),
        // never against request.getCustomerId() - otherwise anyone could write a review
        // "as" someone else's customerId for a booking that isn't theirs.
        if (!booking.getCustomerId().equals(callerId)) {
            throw new ReviewOwnershipException("Bu bron sizə aid deyil, rəy yaza bilməzsiniz.");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BookingNotCompletedException("Yalnız tamamlanmış (COMPLETED) tələblərə rəy yazıla bilər.");
        }
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new ReviewAlreadyExistsException("Bu bron üçün artıq rəy yazılıb.");
        }
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Reytinq 1 ilə 5 arasında olmalıdır.");
        }

        Review review = Review.builder()
                .bookingId(booking.getId())
                .customerId(booking.getCustomerId())
                .artistId(booking.getArtistId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        try {
            authServiceClient.updateArtistRating(booking.getArtistId(), new UpdateArtistRatingRequest(request.getRating()));
        } catch (Exception ex) {
            // The review row is the source of truth and is already saved. The artist's
            // ratingAvg/ratingCount in auth-service is a derived cache - if this call
            // fails (auth-service down, network blip) we don't want to roll back a
            // legitimate review over it. Worth revisiting with a retry/reconciliation
            // job if this matters for the demo. Logged (not swallowed silently) so a
            // real failure here is actually visible instead of just "rating never updates".
            log.error("Rəssamın reytinqi yenilənmədi (artistId={}, rating={}): {}",
                    booking.getArtistId(), request.getRating(), ex.getMessage(), ex);
        }

        return mapToResponse(saved);
    }

    public List<ReviewResponse> getReviewsForArtist(Long artistId) {
        return reviewRepository.findByArtistId(artistId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Usta öz rəyinə ictimai cavab yazır/redaktə edir - artistId review-un öz artistId-si
     *  ilə üst-üstə düşməlidir, əks halda başqa ustanın adından cavab yazıla bilərdi. */
    @Transactional
    public ReviewResponse addReply(Long reviewId, ReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Rəy tapılmadı! ID: " + reviewId));

        if (!review.getArtistId().equals(request.getArtistId())) {
            throw new ReviewOwnershipException("Bu rəy sizə aid deyil, cavab yaza bilməzsiniz.");
        }

        review.setArtistReply(request.getReply());
        review.setRepliedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .customerId(review.getCustomerId())
                .artistId(review.getArtistId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .artistReply(review.getArtistReply())
                .repliedAt(review.getRepliedAt())
                .build();
    }
}
