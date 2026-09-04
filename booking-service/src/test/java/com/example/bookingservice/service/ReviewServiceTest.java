package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.dto.UpdateArtistRatingRequest;
import com.example.bookingservice.dto.ReviewRequest;
import com.example.bookingservice.dto.ReviewResponse;
import com.example.bookingservice.exception.ReviewOwnershipException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.model.Review;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AuthServiceClient authServiceClient;

    private ReviewService reviewService;

    private void init() {
        reviewService = new ReviewService(reviewRepository, bookingRepository, authServiceClient);
    }

    private Booking completedBooking() {
        return Booking.builder().id(1L).customerId(7L).artistId(3L).status(BookingStatus.COMPLETED).build();
    }

    @Test
    void createReview_rejectsWhenCallerIsNotTheBookingsCustomer() {
        init();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(completedBooking()));

        ReviewRequest request = new ReviewRequest();
        request.setBookingId(1L);
        request.setRating(5);

        // caller is 999, but the booking's real customer is 7 - someone trying to
        // review a booking that isn't theirs (previously this was checked against
        // request.getCustomerId(), which the caller also controls - not a real check).
        assertThatThrownBy(() -> reviewService.createReview(request, 999L))
                .isInstanceOf(ReviewOwnershipException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_savesReview_evenWhenRatingSyncToAuthServiceFails() {
        init();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("auth-service unreachable"))
                .when(authServiceClient).updateArtistRating(eq(3L), any(UpdateArtistRatingRequest.class));

        ReviewRequest request = new ReviewRequest();
        request.setBookingId(1L);
        request.setRating(5);
        request.setComment("Əla iş çıxdı.");

        // The review itself is the source of truth - a hiccup talking to auth-service
        // about the derived rating-average cache must not roll back or crash this.
        ReviewResponse response = reviewService.createReview(request, 7L);

        assertThat(response.getRating()).isEqualTo(5);
        verify(reviewRepository).save(any(Review.class));
    }
}
