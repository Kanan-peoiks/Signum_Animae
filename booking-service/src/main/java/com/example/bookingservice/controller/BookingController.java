package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.ArtistStatsDto;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.dto.UpdateBookingPriceRequest;
import com.example.bookingservice.dto.UpdateStatusRequest;
import com.example.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request, request.getCustomerId()));
    }

    /** Restricted to the booking's own customer/artist - a booking's notes/price are
     *  private between the two of them (the "past tattoos" feature uses the separate,
     *  deliberately-public completed-summary endpoint below instead of this one). */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    /** "My orders" list - private, customer-only. */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId));
    }

    /** "My bookings" list as an artist - private, artist-only. */
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(bookingService.getBookingsByArtist(artistId));
    }

    /** Usta analitika paneli - sifariş sayları və qazanc. */
    @GetMapping("/artist/{artistId}/stats")
    public ResponseEntity<ArtistStatsDto> getArtistStats(@PathVariable Long artistId) {
        return ResponseEntity.ok(bookingService.getArtistStats(artistId));
    }

    /**
     * The public "past tattoos" list shown on a customer's profile - anyone logged in
     * may call this (that's the feature), but it never exposes price/notes/reference
     * images. See BookingService.getCompletedSummaryForCustomer.
     */
    @GetMapping("/customer/{customerId}/completed-summary")
    public ResponseEntity<List<CompletedTattooDto>> getCompletedSummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getCompletedSummaryForCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request.getStatus()));
    }

    /**
     * Internal, service-to-service only (chat-service uses this to check whether a
     * booking has been cancelled before allowing a new price OFFER or an OFFER
     * acceptance - see chat-service's ChatMessageService). Guarded by
     * TrustedRequestFilter's "/internal/" rule (a shared X-Internal-Token), not a user
     * identity.
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<BookingResponse> getBookingInternal(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    /**
     * Internal, service-to-service only (called by chat-service via Feign when a
     * customer accepts an OFFER chat message) - guarded by TrustedRequestFilter's
     * "/internal/" rule (a shared X-Internal-Token), not a user identity - there is no
     * end-user token in a server-to-server call.
     */
    @PatchMapping("/internal/{id}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable Long id, @Valid @RequestBody UpdateBookingPriceRequest request) {
        bookingService.updateEstimatedPrice(id, request.getEstimatedPrice());
        return ResponseEntity.ok().build();
    }
}
