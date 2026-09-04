package com.example.authservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial update: any null field here is left unchanged on the existing profile. */
@Data
public class UpdateArtistProfileRequest {
    @Size(max = 2000)
    private String bio;

    @Min(0) @Max(80)
    private Integer experienceYears;

    @Size(max = 300)
    private String styles;
}
