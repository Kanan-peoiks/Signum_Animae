package com.example.bookingservice.dto;

import com.example.bookingservice.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private Long customerId;
    private Long artistId;
    private LocalDateTime bookingDate;
    private BookingStatus status;
    private String notes;
    private String tattooConceptUrl;
    private Double estimatedPrice;
    private LocalDateTime createdAt;
}