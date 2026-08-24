package com.example.authservice.repo;

import com.example.authservice.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByArtistProfileId(Long artistProfileId);
}