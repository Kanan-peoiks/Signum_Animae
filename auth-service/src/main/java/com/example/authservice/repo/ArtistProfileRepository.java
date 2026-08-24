package com.example.authservice.repo;

import com.example.authservice.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    Optional<ArtistProfile> findByUserId(Long userId);

    @Query("SELECT a FROM ArtistProfile a WHERE " +
            "(:city IS NULL OR LOWER(a.user.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
            "(:style IS NULL OR LOWER(a.styles) LIKE LOWER(CONCAT('%', :style, '%'))) AND " +
            "(:minRating IS NULL OR a.ratingAvg >= :minRating)")

    List<ArtistProfile> searchArtists(@Param("city") String city,
                                      @Param("style") String style,
                                      @Param("minRating") Double minRating);
}