package com.example.notificationservice.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Mirrors auth-service's InternalUserContactDto - separate Gradle projects, no shared module. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserContactDto {
    private Long id;
    private String email;
    private String fullName;
}
