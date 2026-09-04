package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /** customerId always comes from the verified caller, never from the request body -
     *  otherwise anyone could open a booking "as" someone else. */
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

    /**
     * Called (via Feign, from chat-service) when a customer accepts an OFFER
     * message in chat - see ChatMessageService.respondToOffer(). This is what
     * makes the chat "price negotiation" actually change the booking record
     * instead of just being decorative chat text.
     */
    public void updateEstimatedPrice(Long id, Double newPrice) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Bron tapılmadı! ID: " + id));
        booking.setEstimatedPrice(newPrice);
        bookingRepository.save(booking);
    }

    /**
     * The "past tattoos" section of a customer's profile - intentionally viewable by
     * ANY logged-in user (that's the feature), but narrowed to COMPLETED bookings only,
     * with no price/reference-image fields, and the artist's name masked to initials
     * unless the viewer (callerId) is premium. Looking up "is the viewer premium" and
     * "what's this artist's name" both happen here, server-side, instead of the
     * frontend deciding after already receiving the real name.
     */
    public List<CompletedTattooDto> getCompletedSummaryForCustomer(Long customerId, Long callerId) {
        List<Booking> completed = bookingRepository.findByCustomerId(customerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            return List.of();
        }

        boolean viewerPremium = isPremium(callerId);
        Map<Long, String> artistNameCache = new HashMap<>();

        return completed.stream()
                .map(b -> CompletedTattooDto.builder()
                        .bookingId(b.getId())
                        .artistId(b.getArtistId())
                        .artistName(resolveArtistName(b.getArtistId(), viewerPremium, artistNameCache))
                        .description(b.getNotes())
                        .bookingDate(b.getBookingDate())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isPremium(Long callerId) {
        try {
            InternalUserSummaryDto viewer = authServiceClient.getUserSummary(callerId);
            return viewer != null && viewer.isPremium();
        } catch (Exception ex) {
            // Fail closed: if auth-service is unreachable, treat the viewer as non-premium
            // rather than accidentally leaking full names.
            log.error("İstifadəçinin premium statusu yoxlanılmadı (callerId={}): {}", callerId, ex.getMessage(), ex);
            return false;
        }
    }

    private String resolveArtistName(Long artistId, boolean viewerPremium, Map<Long, String> cache) {
        String fullName = cache.computeIfAbsent(artistId, id -> {
            try {
                InternalUserSummaryDto artist = authServiceClient.getUserSummary(id);
                return artist != null ? artist.getFullName() : null;
            } catch (Exception ex) {
                log.error("Ustanın adı alınmadı (artistId={}): {}", id, ex.getMessage(), ex);
                return null;
            }
        });
        if (fullName == null || fullName.isBlank()) {
            return "Usta";
        }
        return viewerPremium ? fullName : initialsOf(fullName);
    }

    private String initialsOf(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.length() > 0 ? sb.toString() : "Usta";
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
