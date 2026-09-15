package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformBookingStatsDto {
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long newLast7Days;
    private long newLast30Days;
    private long totalReviews;
    private double averageRating;
    private double totalRevenue;
}
