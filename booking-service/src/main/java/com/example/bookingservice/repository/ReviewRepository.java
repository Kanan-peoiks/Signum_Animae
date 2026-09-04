package com.example.bookingservice.repository;

import com.example.bookingservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);
    List<Review> findByArtistId(Long artistId);
}
