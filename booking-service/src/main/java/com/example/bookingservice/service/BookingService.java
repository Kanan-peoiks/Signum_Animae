package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.NotificationServiceClient;
import com.example.bookingservice.client.dto.NotificationRequest;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.ArtistStatsDto;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.exception.InvalidStatusChangeException;
import com.example.bookingservice.exception.SlotAlreadyTakenException;
import com.example.bookingservice.repository.AvailabilitySlotRepository;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter BOOKING_DAY =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Map<BookingStatus, String> STATUS_AZ = Map.of(
            BookingStatus.PENDING, "Gözləyir",
            BookingStatus.CONFIRMED, "Təsdiqlənib",
            BookingStatus.COMPLETED, "Tamamlanıb",
            BookingStatus.CANCELLED, "Ləğv edilib");

    private final BookingRepository bookingRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AuthServiceClient authServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    /** Yeri tutan vəziyyətlər - ləğv edilmiş və tamamlanmış sifarişlər vaxtı boşaldır. */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    public BookingResponse createBooking(BookingRequest request, Long callerId) {
        if (bookingRepository.existsByArtistIdAndBookingDateAndStatusIn(
                request.getArtistId(), request.getBookingDate(), ACTIVE_STATUSES)) {
            throw new SlotAlreadyTakenException(
                    "Bu vaxt artıq tutulub. Zəhmət olmasa başqa vaxt seç.");
        }

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
        markSlot(saved.getArtistId(), saved.getBookingDate(), true);

        notifyQuietly(saved.getArtistId(), "Yeni sifariş",
                customerName(callerId) + " sizə sifariş göndərdi.");

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

        checkStatusChange(booking, newStatus, callerId);

        booking.setStatus(newStatus);
        Booking updated = bookingRepository.save(booking);

        // Ləğv olunan vaxt yenidən boşalır, tamamlanan vaxt isə keçmişdə qalır
        if (newStatus == BookingStatus.CANCELLED) {
            markSlot(updated.getArtistId(), updated.getBookingDate(), false);
        }

        Long peerId = callerId.equals(updated.getCustomerId())
                ? updated.getArtistId()
                : updated.getCustomerId();
        notifyQuietly(peerId, "Sifariş vəziyyəti dəyişdi",
                updated.getBookingDate().format(BOOKING_DAY) + " tarixli sifariş → "
                        + STATUS_AZ.getOrDefault(newStatus, newStatus.name()));

        return mapToResponse(updated);
    }

    /* Vəziyyət qaydaları. Əvvəllər heç bir qayda yox idi: ləğv edilmiş sifariş
       yenidən təsdiqlənə, müştəri isə sifarişi özü "tamamlandı" edə bilirdi. */
    private void checkStatusChange(Booking booking, BookingStatus newStatus, Long callerId) {
        BookingStatus current = booking.getStatus();

        if (current == BookingStatus.CANCELLED || current == BookingStatus.COMPLETED) {
            throw new InvalidStatusChangeException(
                    "Bu sifariş artıq bağlanıb (" + STATUS_AZ.getOrDefault(current, current.name())
                            + "), vəziyyəti dəyişdirmək olmaz.");
        }
        if (newStatus == current) {
            throw new InvalidStatusChangeException("Sifariş onsuz da bu vəziyyətdədir.");
        }

        boolean isArtist = callerId.equals(booking.getArtistId());

        switch (newStatus) {
            case CONFIRMED -> {
                if (!isArtist) {
                    throw new InvalidStatusChangeException("Sifarişi yalnız usta təsdiqləyə bilər.");
                }
                if (current != BookingStatus.PENDING) {
                    throw new InvalidStatusChangeException("Yalnız gözləyən sifariş təsdiqlənə bilər.");
                }
            }
            case COMPLETED -> {
                if (!isArtist) {
                    throw new InvalidStatusChangeException("Sifarişi yalnız usta tamamlaya bilər.");
                }
                if (current != BookingStatus.CONFIRMED) {
                    throw new InvalidStatusChangeException(
                            "Yalnız təsdiqlənmiş sifariş tamamlana bilər.");
                }
            }
            case CANCELLED -> { /* həm müştəri, həm usta ləğv edə bilər */ }
            case PENDING -> throw new InvalidStatusChangeException(
                    "Sifarişi yenidən gözləmə vəziyyətinə qaytarmaq olmaz.");
        }
    }

    /* Sifariş verilən vaxt ustanın uyğunluq pəncərəsinə düşürsə, o pəncərə
       tutulur və açıq siyahıda görünmür; ləğv olunanda yenidən açılır. */
    private void markSlot(Long artistId, LocalDateTime bookingDate, boolean booked) {
        availabilitySlotRepository.findByArtistIdAndSlotStart(artistId, bookingDate)
                .ifPresent(slot -> {
                    slot.setBooked(booked);
                    availabilitySlotRepository.save(slot);
                });
    }

    private String customerName(Long customerId) {
        try {
            InternalUserSummaryDto summary = authServiceClient.getUserSummary(customerId);
            if (summary != null && summary.getFullName() != null && !summary.getFullName().isBlank()) {
                return summary.getFullName();
            }
        } catch (Exception ex) {
            log.error("Müştərinin adı alınmadı (customerId={}): {}", customerId, ex.getMessage(), ex);
        }
        return "Bir müştəri";
    }

    private void notifyQuietly(Long userId, String title, String message) {
        try {
            notificationServiceClient.send(NotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .sendEmail(true)
                    .build());
        } catch (Exception ex) {
            log.error("Bildiriş göndərilmədi (userId={}, title={}): {}", userId, title, ex.getMessage(), ex);
        }
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
