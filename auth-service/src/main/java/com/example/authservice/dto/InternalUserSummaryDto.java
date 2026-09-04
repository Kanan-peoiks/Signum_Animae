package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Server-to-server only (see UserController's /internal/{id}) - deliberately excludes
 * email and everything else a caller doesn't need just to decide "is this user premium"
 * or "what's their display name". Never returned to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserSummaryDto {
    private Long id;
    private String fullName;
    private boolean premium;
}
