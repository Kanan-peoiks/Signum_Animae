package com.example.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    @Size(max = 150)
    private String fullName;

    @Size(max = 150)
    private String city;

    @Size(max = 500)
    private String profileImageUrl;
}
