package com.example.bookingservice.repository;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByArtistId(Long artistId);
    List<Booking> findByArtistIdAndStatus(Long artistId, BookingStatus status);

    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);
    Page<Booking> findByArtistId(Long artistId, Pageable pageable);

    /* Eyni usta, eyni vaxt: ləğv/tamamlanmış sifarişlər yeri tutmur. */
    boolean existsByArtistIdAndBookingDateAndStatusIn(
            Long artistId, LocalDateTime bookingDate, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT COALESCE(SUM(b.estimatedPrice), 0) FROM Booking b WHERE b.status = :status")
    double sumEstimatedPriceByStatus(@Param("status") BookingStatus status);
}
