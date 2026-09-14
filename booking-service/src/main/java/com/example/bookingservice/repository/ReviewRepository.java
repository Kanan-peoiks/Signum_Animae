package com.example.bookingservice.repository;

import com.example.bookingservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);
    List<Review> findByArtistId(Long artistId);

    /** Platforma üzrə orta reytinq. Heç bir rəy yoxdursa AVG null qaytarır,
     *  ona görə primitiv double yox, Double. */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double averageRating();
}
