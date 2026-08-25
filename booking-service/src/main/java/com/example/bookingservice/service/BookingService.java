package com.example.bookingservice.service;

import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingResponse createBooking(BookingRequest request) {
        Booking booking = Booking.builder()
                .customerId(request.getCustomerId())
                .artistId(request.getArtistId())
                .bookingDate(request.getBookingDate())
                .notes(request.getNotes())
                .tattooConceptUrl(request.getTattooConceptUrl())
                .estimatedPrice(request.getEstimatedPrice())
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Bron tapılmadı! ID: " + id));
        return mapToResponse(booking);
    }

    public List<BookingResponse> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByArtist(Long artistId) {
        return bookingRepository.findByArtistId(artistId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse updateBookingStatus(Long id, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Bron tapılmadı! ID: " + id));

        booking.setStatus(newStatus);
        Booking updated = bookingRepository.save(booking);
        return mapToResponse(updated);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomerId())
                .artistId(booking.getArtistId())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .notes(booking.getNotes())
                .tattooConceptUrl(booking.getTattooConceptUrl())
                .estimatedPrice(booking.getEstimatedPrice())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}