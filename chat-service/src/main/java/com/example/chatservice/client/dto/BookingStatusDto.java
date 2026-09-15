package com.example.chatservice.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusDto {
    private Long id;
    private Long customerId;
    private Long artistId;
    private String status;
}
