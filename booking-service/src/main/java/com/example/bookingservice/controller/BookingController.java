package com.example.bookingservice.controller;

import com.example.bookingservice.config.GatewayHeaders;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.ArtistStatsDto;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.dto.PageParams;
import com.example.bookingservice.dto.PageResponse;
import com.example.bookingservice.dto.UpdateBookingPriceRequest;
import com.example.bookingservice.dto.UpdateStatusRequest;
import com.example.bookingservice.security.AccessGuard;
import com.example.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(bookingService.createBooking(request, callerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(bookingService.getBookingByIdForCaller(id, callerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PageResponse<BookingResponse>> getBookingsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(customerId, callerId);
        return ResponseEntity.ok(PageResponse.from(
                bookingService.getBookingsByCustomer(customerId, PageParams.of(page, size, NEWEST_FIRST))));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<PageResponse<BookingResponse>> getBookingsByArtist(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(artistId, callerId);
        return ResponseEntity.ok(PageResponse.from(
                bookingService.getBookingsByArtist(artistId, PageParams.of(page, size, NEWEST_FIRST))));
    }

    @GetMapping("/artist/{artistId}/stats")
    public ResponseEntity<ArtistStatsDto> getArtistStats(
            @PathVariable Long artistId,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        AccessGuard.requireSelf(artistId, callerId);
        return ResponseEntity.ok(bookingService.getArtistStats(artistId));
    }

    @GetMapping("/customer/{customerId}/completed-summary")
    public ResponseEntity<List<CompletedTattooDto>> getCompletedSummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getCompletedSummaryForCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long callerId) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request.getStatus(), callerId));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<BookingResponse> getBookingInternal(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PatchMapping("/internal/{id}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable Long id, @Valid @RequestBody UpdateBookingPriceRequest request) {
        bookingService.updateEstimatedPrice(id, request.getEstimatedPrice());
        return ResponseEntity.ok().build();
    }
}
