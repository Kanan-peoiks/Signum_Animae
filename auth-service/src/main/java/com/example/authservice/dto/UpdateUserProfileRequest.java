package com.example.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial update: any null field here is left unchanged on the existing user. Password/email/role are not editable here on purpose. */
@Data
public class UpdateUserProfileRequest {
    @Size(max = 150)
    private String fullName;

    @Size(max = 150)
    private String city;

    @Size(max = 500)
    private String profileImageUrl;
}
