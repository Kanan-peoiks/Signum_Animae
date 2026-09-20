package com.example.bookingservice.service;

import com.example.bookingservice.client.AuthServiceClient;
import com.example.bookingservice.client.NotificationServiceClient;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CompletedTattooDto;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.exception.InvalidStatusChangeException;
import com.example.bookingservice.exception.SlotAlreadyTakenException;
import com.example.bookingservice.repository.AvailabilitySlotRepository;
import com.example.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private NotificationServiceClient notificationServiceClient;
    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_takesCustomerIdFromTheVerifiedCaller() {
        BookingRequest request = new BookingRequest();
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
    void completedSummary_returnsArtistFullNameAndNoPrice() {
        Booking completed = Booking.builder()
                .id(10L).customerId(7L).artistId(3L)
                .status(BookingStatus.COMPLETED)
                .notes("Kürəkdə portret")
                .estimatedPrice(450.0)
                .bookingDate(LocalDateTime.now().minusDays(3))
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(completed));
        when(authServiceClient.getUserSummary(3L))
                .thenReturn(new InternalUserSummaryDto(3L, "Nihat İnk"));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistName()).isEqualTo("Nihat İnk");
        assertThat(result.get(0).getDescription()).isEqualTo("Kürəkdə portret");
    }

    @Test
    void completedSummary_excludesNonCompletedBookings() {
        Booking pending = Booking.builder()
                .id(12L).customerId(7L).artistId(3L)
                .status(BookingStatus.PENDING)
                .bookingDate(LocalDateTime.now())
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(pending));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L);

        assertThat(result).isEmpty();
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void completedSummary_authServiceDown_fallsBackToGenericArtistLabel() {
        Booking completed = Booking.builder()
                .id(13L).customerId(7L).artistId(3L)
                .status(BookingStatus.COMPLETED)
                .bookingDate(LocalDateTime.now())
                .build();
        when(bookingRepository.findByCustomerId(7L)).thenReturn(List.of(completed));
        when(authServiceClient.getUserSummary(eq(3L))).thenThrow(new RuntimeException("auth-service down"));

        List<CompletedTattooDto> result = bookingService.getCompletedSummaryForCustomer(7L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistName()).isEqualTo("Usta");
    }

    /* Eyni vaxta ikinci sifariş: əvvəllər heç bir yoxlama yox idi. */
    @Test
    void createBooking_refusesATimeThatIsAlreadyTaken() {
        LocalDateTime when = LocalDateTime.now().plusDays(2);
        BookingRequest request = new BookingRequest();
        request.setArtistId(5L);
        request.setBookingDate(when);

        when(bookingRepository.existsByArtistIdAndBookingDateAndStatusIn(eq(5L), eq(when), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(request, 42L))
                .isInstanceOf(SlotAlreadyTakenException.class);

        verify(bookingRepository, never()).save(any());
    }

    private Booking booking(BookingStatus status) {
        return Booking.builder()
                .id(9L).customerId(42L).artistId(5L)
                .bookingDate(LocalDateTime.now().plusDays(3))
                .status(status)
                .build();
    }

    /* Müştəri ləğv edəndən sonra usta prosesi davam etdirə bilirdi. */
    @Test
    void updateStatus_refusesToReopenACancelledBooking() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(booking(BookingStatus.CANCELLED)));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(9L, BookingStatus.CONFIRMED, 5L))
                .isInstanceOf(InvalidStatusChangeException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateStatus_refusesWhenTheCustomerCompletesTheirOwnBooking() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(9L, BookingStatus.COMPLETED, 42L))
                .isInstanceOf(InvalidStatusChangeException.class);

        verify(bookingRepository, never()).save(any());
    }

    /* Ləğv olunan vaxt ustanın açıq pəncərələrinə geri qayıtmalıdır. */
    @Test
    void updateStatus_freesTheSlotWhenTheBookingIsCancelled() {
        Booking existing = booking(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(availabilitySlotRepository.findByArtistIdAndSlotStart(5L, existing.getBookingDate()))
                .thenReturn(Optional.empty());

        bookingService.updateBookingStatus(9L, BookingStatus.CANCELLED, 42L);

        verify(availabilitySlotRepository).findByArtistIdAndSlotStart(5L, existing.getBookingDate());
    }
}
