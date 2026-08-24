package com.example.authservice.service;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.model.ArtistProfile;
import com.example.authservice.repo.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistProfileRepository artistProfileRepository;

    public List<ArtistProfileDto> searchArtists(String city, String style, Double minRating) {
        return artistProfileRepository.searchArtists(city, style, minRating)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ArtistProfileDto mapToDto(ArtistProfile profile) {
        return ArtistProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .fullName(profile.getUser().getFullName())
                .city(profile.getUser().getCity())
                .bio(profile.getBio())
                .experienceYears(profile.getExperienceYears())
                .styles(profile.getStyles())
                .ratingAvg(profile.getRatingAvg())
                .ratingCount(profile.getRatingCount())
                .build();
    }
}