package com.example.authservice.service;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.dto.UpdateArtistProfileRequest;
import com.example.authservice.exception.ArtistNotFoundException;
import com.example.authservice.model.ArtistProfile;
import com.example.authservice.repo.ArtistProfileRepository;
import com.example.authservice.repo.ArtistSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistPopularityService artistPopularityService;

    public Page<ArtistProfileDto> searchArtists(String city, String style, Double minRating,
                                                Integer minExperienceYears, String sortBy,
                                                Pageable pageable) {
        List<Specification<ArtistProfile>> filters = new ArrayList<>();

        Specification<ArtistProfile> citySpec = ArtistSpecifications.hasCity(city);
        if (citySpec != null) {
            filters.add(citySpec);
        }
        Specification<ArtistProfile> styleSpec = ArtistSpecifications.hasStyle(style);
        if (styleSpec != null) {
            filters.add(styleSpec);
        }
        Specification<ArtistProfile> ratingSpec = ArtistSpecifications.minRating(minRating);
        if (ratingSpec != null) {
            filters.add(ratingSpec);
        }
        Specification<ArtistProfile> expSpec = ArtistSpecifications.minExperience(minExperienceYears);
        if (expSpec != null) {
            filters.add(expSpec);
        }

        Specification<ArtistProfile> spec = Specification.allOf(filters);

        Sort sort = "rating".equalsIgnoreCase(sortBy) ? Sort.by(Sort.Direction.DESC, "ratingAvg")
                : "experience".equalsIgnoreCase(sortBy) ? Sort.by(Sort.Direction.DESC, "experienceYears")
                : Sort.unsorted();

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return artistProfileRepository.findAll(spec, sorted).map(this::toDto);
    }

    public ArtistProfileDto getArtistByUserId(Long artistUserId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistUserId)
                .orElseThrow(() -> new ArtistNotFoundException("Rəssam tapılmadı! userId: " + artistUserId));
        artistPopularityService.recordView(artistUserId);
        return toDto(profile);
    }

    public long getViewCount(Long artistUserId) {
        return artistPopularityService.getViewCount(artistUserId);
    }

    public List<ArtistProfileDto> getPopularArtists(int limit) {
        Set<Long> artistUserIds = artistPopularityService.getPopularArtistIds(limit);
        List<ArtistProfileDto> result = new ArrayList<>();
        for (Long userId : artistUserIds) {
            artistProfileRepository.findByUserId(userId).ifPresent(profile -> result.add(toDto(profile)));
        }
        return result;
    }

    public void updateRatingAfterReview(Long artistUserId, int newRating) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistUserId)
                .orElseThrow(() -> new ArtistNotFoundException("Rəssam tapılmadı! userId: " + artistUserId));

        int oldCount = profile.getRatingCount() == null ? 0 : profile.getRatingCount();
        double oldAvg = profile.getRatingAvg() == null ? 0.0 : profile.getRatingAvg();

        int newCount = oldCount + 1;
        double newAvg = ((oldAvg * oldCount) + newRating) / newCount;

        profile.setRatingCount(newCount);
        profile.setRatingAvg(newAvg);
        artistProfileRepository.save(profile);
    }

    public ArtistProfileDto updateProfile(Long artistUserId, UpdateArtistProfileRequest request) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistUserId)
                .orElseThrow(() -> new ArtistNotFoundException("Rəssam tapılmadı! userId: " + artistUserId));

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getExperienceYears() != null) {
            profile.setExperienceYears(request.getExperienceYears());
        }
        if (request.getStyles() != null) {
            profile.setStyles(request.getStyles());
        }

        ArtistProfile saved = artistProfileRepository.save(profile);
        return toDto(saved);
    }

    public ArtistProfileDto toDto(ArtistProfile profile) {
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
