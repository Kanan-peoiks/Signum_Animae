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
 *
 * artistName is already masked to initials server-side when the VIEWER isn't premium -
 * the frontend used to do this masking itself after fetching full names for everyone,
 * which meant the real name was sitting in the network response the whole time. Now the
 * server only ever sends what the viewer is allowed to see.
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
