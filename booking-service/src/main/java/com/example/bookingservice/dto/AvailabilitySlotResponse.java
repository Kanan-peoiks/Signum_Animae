package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlotResponse {
    private Long id;
    private Long artistId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private boolean booked;
}
