package com.example.bookingservice.model;

/**
 * 4 states, matching the original project spec (simplified from an earlier
 * 5-state version that also had REJECTED): a booking a customer requested is
 * PENDING until the artist responds; the artist either CONFIRMED-s it or
 * CANCELLED-s it (an artist declining before ever confirming and an artist/
 * customer cancelling later are the same outcome from the system's point of
 * view, so they share one status instead of two); once the tattoo session
 * actually happens the artist marks it COMPLETED, which is what unlocks
 * leaving a review (see ReviewService.createReview).
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
