package com.example.bookingservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    private Long customerId;
    private Long artistId;
    private LocalDateTime bookingDate;
    private String notes;
    private String tattooConceptUrl;
    private Double estimatedPrice;
}
