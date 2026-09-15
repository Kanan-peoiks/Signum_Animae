package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.ArtistStatsDto;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AuthServiceClient authServiceClient;

    public BookingResponse createBooking(BookingRequest request, Long callerId) {
        Booking booking = Booking.builder()
                .customerId(callerId)
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
        return mapToResponse(findOrThrow(id));
    }

    public BookingResponse getBookingByIdForCaller(Long id, Long callerId) {
        Booking booking = findOrThrow(id);
        AccessGuard.requireOneOf(callerId, booking.getCustomerId(), booking.getArtistId(),
                "Bu sifariş sizə aid deyil.");
        return mapToResponse(booking);
    }

    public Page<BookingResponse> getBookingsByCustomer(Long customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
    }

    public Page<BookingResponse> getBookingsByArtist(Long artistId, Pageable pageable) {
        return bookingRepository.findByArtistId(artistId, pageable).map(this::mapToResponse);
    }

    public ArtistStatsDto getArtistStats(Long artistId) {
        List<Booking> bookings = bookingRepository.findByArtistId(artistId);

        long pending = 0, confirmed = 0, completed = 0, cancelled = 0;
        double earnings = 0;
        for (Booking b : bookings) {
            switch (b.getStatus()) {
                case PENDING -> pending++;
                case CONFIRMED -> confirmed++;
                case COMPLETED -> {
                    completed++;
                    if (b.getEstimatedPrice() != null) {
                        earnings += b.getEstimatedPrice();
                    }
                }
                case CANCELLED -> cancelled++;
            }
        }

        return ArtistStatsDto.builder()
                .totalBookings(bookings.size())
                .pendingBookings(pending)
                .confirmedBookings(confirmed)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .totalEarnings(earnings)
                .build();
    }

    public BookingResponse updateBookingStatus(Long id, BookingStatus newStatus, Long callerId) {
        Booking booking = findOrThrow(id);
        AccessGuard.requireOneOf(callerId, booking.getCustomerId(), booking.getArtistId(),
                "Bu sifariş sizə aid deyil.");

        booking.setStatus(newStatus);
        Booking updated = bookingRepository.save(booking);
        return mapToResponse(updated);
    }

    public void updateEstimatedPrice(Long id, Double newPrice) {
        Booking booking = findOrThrow(id);
        booking.setEstimatedPrice(newPrice);
        bookingRepository.save(booking);
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Bron tapılmadı! ID: " + id));
    }

    public List<CompletedTattooDto> getCompletedSummaryForCustomer(Long customerId) {
        List<Booking> completed = bookingRepository.findByCustomerId(customerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            return List.of();
        }

        Map<Long, String> artistNameCache = new HashMap<>();

        return completed.stream()
                .map(b -> CompletedTattooDto.builder()
                        .bookingId(b.getId())
                        .artistId(b.getArtistId())
                        .artistName(resolveArtistName(b.getArtistId(), artistNameCache))
                        .description(b.getNotes())
                        .bookingDate(b.getBookingDate())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveArtistName(Long artistId, Map<Long, String> cache) {
        String fullName = cache.computeIfAbsent(artistId, id -> {
            try {
                InternalUserSummaryDto artist = authServiceClient.getUserSummary(id);
                return artist != null ? artist.getFullName() : null;
            } catch (Exception ex) {
                log.error("Ustanın adı alınmadı (artistId={}): {}", id, ex.getMessage(), ex);
                return null;
            }
        });
        return (fullName == null || fullName.isBlank()) ? "Usta" : fullName;
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
