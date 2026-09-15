package com.example.bookingservice.service;

import com.example.bookingservice.dto.PlatformBookingStatsDto;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PlatformBookingStatsDto bookingStats() {
        LocalDateTime now = LocalDateTime.now();
        Double avg = reviewRepository.averageRating();

        return PlatformBookingStatsDto.builder()
                .totalBookings(bookingRepository.count())
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .confirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .completedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .newLast7Days(bookingRepository.countByCreatedAtAfter(now.minusDays(7)))
                .newLast30Days(bookingRepository.countByCreatedAtAfter(now.minusDays(30)))
                .totalReviews(reviewRepository.count())
                .averageRating(avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0)
                .totalRevenue(bookingRepository.sumEstimatedPriceByStatus(BookingStatus.COMPLETED))
                .build();
    }
}
