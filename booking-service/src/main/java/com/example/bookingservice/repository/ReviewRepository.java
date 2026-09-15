package com.example.bookingservice.repository;

import com.example.bookingservice.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);
    List<Review> findByArtistId(Long artistId);

    Page<Review> findByArtistId(Long artistId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double averageRating();
}
