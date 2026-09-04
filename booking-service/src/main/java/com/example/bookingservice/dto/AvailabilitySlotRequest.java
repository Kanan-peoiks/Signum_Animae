package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvailabilitySlotRequest {
    @NotNull(message = "artistId tələb olunur")
    private Long artistId;

    @NotNull(message = "Başlanğıc vaxtı tələb olunur")
    private LocalDateTime slotStart;

    @NotNull(message = "Bitmə vaxtı tələb olunur")
    private LocalDateTime slotEnd;
}
