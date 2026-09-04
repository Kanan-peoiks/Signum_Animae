package com.example.authservice.repo;

import com.example.authservice.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long>, JpaSpecificationExecutor<ArtistProfile> {
    Optional<ArtistProfile> findByUserId(Long userId);
}
