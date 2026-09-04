package com.example.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors auth-service's InternalUserSummaryDto - separate Gradle projects, no shared module. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserSummaryDto {
    private Long id;
    private String fullName;
}
