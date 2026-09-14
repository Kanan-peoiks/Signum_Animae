package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin paneli üçün platforma səviyyəsində bron/rəy mənzərəsi.
 *  ArtistStatsDto tək ustaya aiddir, bu isə bütün platformanı əhatə edir. */
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
    /** Tamamlanmış bronların estimatedPrice cəmi - təxmini dövriyyə. */
    private double totalRevenue;
}
