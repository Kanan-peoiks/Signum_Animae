package com.example.chatservice.client.dto;

import lombok.Data;

/** Separate copy of booking-service's own DTO of the same shape - services don't share code in this project. */
@Data
public class UpdateBookingPriceRequest {
    private Double estimatedPrice;
}
