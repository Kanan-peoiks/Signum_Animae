package com.example.authservice.repo;

import com.example.authservice.model.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {

    boolean existsByCustomerIdAndArtistId(Long customerId, Long artistId);

    Optional<ArtistFollow> findByCustomerIdAndArtistId(Long customerId, Long artistId);

    /** Ən son izlənən əvvəldə - "İzlədiklərim" səhifəsi bu sıra ilə göstərir. */
    List<ArtistFollow> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByArtistId(Long artistId);
}
