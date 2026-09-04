package com.example.chatservice.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Mirrors just the fields chat-service needs from booking-service's BookingResponse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusDto {
    private Long id;
    private Long customerId;
    private Long artistId;
    private String status;
}
