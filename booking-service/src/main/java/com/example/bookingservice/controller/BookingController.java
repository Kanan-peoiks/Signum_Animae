package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.UpdateStatusRequest;
import com.example.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(bookingService.getBookingsByArtist(artistId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request.getStatus()));
    }
}
