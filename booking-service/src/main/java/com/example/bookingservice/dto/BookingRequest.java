package com.example.bookingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    /** Artıq etibar edilmir - customerId həmişə çağıranın özündən (X-User-Id) götürülür,
     *  bax BookingController/BookingService.createBooking. */
    private Long customerId;

    @NotNull(message = "artistId tələb olunur")
    private Long artistId;

    @NotNull(message = "Tarix tələb olunur")
    @Future(message = "Sifariş tarixi gələcəkdə olmalıdır")
    private LocalDateTime bookingDate;

    @Size(max = 2000)
    private String notes;

    @Size(max = 500)
    private String tattooConceptUrl;

    private Double estimatedPrice;
}
