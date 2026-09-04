package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Server-to-server only (see UserController's /internal/{id}/contact) - used by
 * notification-service to resolve who to actually email, instead of trusting a
 * frontend-supplied email address (which would mean any user could ask notification-
 * service to email an arbitrary address "as" someone else, or read someone else's
 * email by watching what the frontend sends). Never returned to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserContactDto {
    private Long id;
    private String email;
    private String fullName;
}
