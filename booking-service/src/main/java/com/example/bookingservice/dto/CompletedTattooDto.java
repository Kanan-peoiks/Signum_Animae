package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The public "past tattoos" view of a completed booking - used on another customer's
 * profile (see BookingService.getCompletedSummaryForCustomer). Deliberately narrower
 * than BookingResponse: no estimatedPrice, no tattooConceptUrl - a stranger browsing
 * someone's profile has no business seeing what they paid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTattooDto {
    private Long bookingId;
    private Long artistId;
    private String artistName;
    private String description;
    private LocalDateTime bookingDate;
}
