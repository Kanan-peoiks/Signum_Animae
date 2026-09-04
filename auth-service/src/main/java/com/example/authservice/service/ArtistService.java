package com.example.authservice.service;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.dto.UpdateArtistProfileRequest;
import com.example.authservice.exception.ArtistNotFoundException;
import com.example.authservice.model.ArtistProfile;
import com.example.authservice.repo.ArtistProfileRepository;
import com.example.authservice.repo.ArtistSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistPopularityService artistPopularityService;

    public List<ArtistProfileDto> searchArtists(String city, String style, Double minRating) {
        // Spring Data JPA 4.x's Specification.where()/.and() throw on a null argument now
        // (Assert.notNull inside), unlike older versions where null meant "no restriction".
        // So we filter out the filters that weren't supplied ourselves, then combine only
        // the ones that exist via Specification.allOf(...). An empty list is fine: allOf()
        // falls back to Specification.unrestricted(), i.e. "match everything".
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

        Specification<ArtistProfile> spec = Specification.allOf(filters);

        return artistProfileRepository.findAll(spec)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * NOTE: {@code artistUserId} is the artist's USER id - the same id used everywhere
     * else in the system as "artistId" (Booking.artistId, chat participant ids,
     * Notification.userId), NOT ArtistProfile.id. Keeping this consistent matters:
     * booking-service and chat-service only ever know the artist by their user id.
     */
    public ArtistProfileDto getArtistByUserId(Long artistUserId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistUserId)
                .orElseThrow(() -> new ArtistNotFoundException("Rəssam tapılmadı! userId: " + artistUserId));
        artistPopularityService.recordView(artistUserId);
        return mapToDto(profile);
    }

    public List<ArtistProfileDto> getPopularArtists(int limit) {
        Set<Long> artistUserIds = artistPopularityService.getPopularArtistIds(limit);
        List<ArtistProfileDto> result = new ArrayList<>();
        for (Long userId : artistUserIds) {
            artistProfileRepository.findByUserId(userId).ifPresent(profile -> result.add(mapToDto(profile)));
        }
        return result;
    }

    /**
     * Called (via Feign, from booking-service) whenever a new review is created for a
     * COMPLETED booking. Recomputes the running average incrementally rather than
     * re-aggregating all reviews, since booking-service - not auth-service - is the
     * source of truth for review rows.
     */
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


    /**
     * The self-service gap this closes: before this, an ArtistProfile row was
     * only ever created (empty) at registration and updated internally after a
     * review - there was no way for the artist to actually fill in their own
     * bio/styles/experience. Partial update: a null field in the request leaves
     * the existing value untouched, so the frontend can send just the one field
     * that changed.
     */
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
        return mapToDto(saved);
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
