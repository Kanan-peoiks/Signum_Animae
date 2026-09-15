package com.example.authservice.repo;

import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByBannedTrue();

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT u.city, COUNT(u) FROM User u WHERE u.city IS NOT NULL AND u.city <> '' " +
           "GROUP BY u.city ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByCity(Pageable pageable);
}
