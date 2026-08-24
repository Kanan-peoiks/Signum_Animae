package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProfileDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String city;
    private String bio;
    private Integer experienceYears;
    private String styles;
    private Double ratingAvg;
    private Integer ratingCount;
}