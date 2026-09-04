package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Usta üçün analitika paneli - sadəcə booking-service-in gördüyü hissə (sifariş
 *  sayları və qazanc). Chat-service-in təklif statistikası ayrıca sorğu ilə gəlir,
 *  frontend ikisini birləşdirir - servislər arası əlavə Feign asılılığı yaratmamaq üçün. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistStatsDto {
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double totalEarnings;
}
