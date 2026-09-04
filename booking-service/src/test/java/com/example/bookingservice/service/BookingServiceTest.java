package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers two things that used to have NO automated check at all:
 *  1. createBooking must use the verified caller as customerId, never whatever the
 *     request body claims (see BookingController/BookingService.createBooking).
 *  2. the "past tattoos" completed-summary endpoint must never leak price/notes-as-
 *     price and must mask the artist's name to initials for a non-premium viewer.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuthServiceClient authServiceClient;
    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_usesCallerIdAsCustomerId_ignoringRequestBody() {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(999L); // an attacker trying to book "as" someone else
        request.setArtistId(5L);
        request.setBookingDate(LocalDateTime.now().plusDays(1));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, 42L);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(42L);
        assertThat(response.getCustomerId()).isEqualTo(42L);
    }

    @Test
    void completedSummary_nonPremiumViewer_getsInitialsOnly_andNoPrice() {
        Booking completed = Booking.builder()
                .id(10L).customerId(7L).artistId(3L)
                .status(BookingStatus.COMPLETED)
                .notes("Kürəkdə portret")
                .estimatedPrice(450.0)
                .bookingDate(LocalDateTime.now().minusDays(3))
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(completed));
        when(authServiceClient.getUserSummary(99L))
                .thenReturn(new InternalUserSummaryDto(99L, "Nihat İnk", false)); // viewer: not premium
        when(authServiceClient.getUserSummary(3L))
                .thenReturn(new InternalUserSummaryDto(3L, "Nihat İnk", false)); // the artist

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L, 99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistName()).isEqualTo("Nİ"); // initials, not the full name
        assertThat(result.get(0).getDescription()).isEqualTo("Kürəkdə portret");
        // CompletedTattooDto has no price field at all - this would fail to compile if
        // someone "helpfully" added estimatedPrice back to it without a masking rule.
    }

    @Test
    void completedSummary_premiumViewer_getsFullArtistName() {
        Booking completed = Booking.builder()
                .id(11L).customerId(7L).artistId(3L)
                .status(BookingStatus.COMPLETED)
                .notes("Bilək üzərində kiçik ilan")
                .estimatedPrice(200.0)
                .bookingDate(LocalDateTime.now().minusDays(10))
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(completed));
        when(authServiceClient.getUserSummary(99L))
                .thenReturn(new InternalUserSummaryDto(99L, "Aygün Məmmədova", true)); // viewer: premium
        when(authServiceClient.getUserSummary(3L))
                .thenReturn(new InternalUserSummaryDto(3L, "Nihat İnk", false));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L, 99L);

        assertThat(result.get(0).getArtistName()).isEqualTo("Nihat İnk");
    }

    @Test
    void completedSummary_excludesNonCompletedBookings() {
        Booking pending = Booking.builder()
                .id(12L).customerId(7L).artistId(3L)
                .status(BookingStatus.PENDING)
                .bookingDate(LocalDateTime.now())
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(pending));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L, 99L);

        assertThat(result).isEmpty();
        verifyNoInteractions(authServiceClient); // no bookings to describe -> no need to even ask
    }

    @Test
    void completedSummary_authServiceDown_failsClosedToNonPremium() {
        Booking completed = Booking.builder()
                .id(13L).customerId(7L).artistId(3L)
                .status(BookingStatus.COMPLETED)
                .bookingDate(LocalDateTime.now())
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(completed));
        when(authServiceClient.getUserSummary(eq(99L))).thenThrow(new RuntimeException("auth-service down"));
        when(authServiceClient.getUserSummary(eq(3L))).thenThrow(new RuntimeException("auth-service down"));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L, 99L);

        // Never blows up, and never accidentally reveals a full name when it can't even
        // confirm the viewer is premium.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistName()).isEqualTo("Usta");
    }
}
