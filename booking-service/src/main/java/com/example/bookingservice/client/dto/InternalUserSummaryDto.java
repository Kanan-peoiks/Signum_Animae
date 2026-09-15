package com.example.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserSummaryDto {
    private Long id;
    private String fullName;
}
