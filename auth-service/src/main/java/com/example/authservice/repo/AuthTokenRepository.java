package com.example.authservice.repo;

import com.example.authservice.model.AuthToken;
import com.example.authservice.model.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByToken(String token);

    List<AuthToken> findByUserIdAndTypeAndUsedFalse(Long userId, AuthTokenType type);
}
