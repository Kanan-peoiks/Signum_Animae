package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTattooDto {
    private Long bookingId;
    private Long artistId;
    private String artistName;
    private String description;
    private LocalDateTime bookingDate;
}
