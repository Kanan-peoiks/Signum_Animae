package com.example.bookingservice.repository;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    /* Limitsiz variantlar - analitika/xülasə hesablamaları bütün sətirləri görməlidir
       (bax getArtistStats, getCompletedSummaryForCustomer), səhifələnmə onlara aid deyil. */
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByArtistId(Long artistId);
    List<Booking> findByArtistIdAndStatus(Long artistId, BookingStatus status);

    /* Ekranda göstərilən siyahılar üçün səhifələnmiş variantlar. */
    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);
    Page<Booking> findByArtistId(Long artistId, Pageable pageable);

    /* ---- admin platform statistikası ---- */

    long countByStatus(BookingStatus status);

    long countByCreatedAtAfter(LocalDateTime since);

    /** estimatedPrice nullable-dır (qiymət hələ razılaşdırılmaya bilər) - COALESCE
     *  həm boş cədvəldə, həm də qiyməti olmayan sətirlərdə 0 qaytarır. */
    @Query("SELECT COALESCE(SUM(b.estimatedPrice), 0) FROM Booking b WHERE b.status = :status")
    double sumEstimatedPriceByStatus(@Param("status") BookingStatus status);
}
