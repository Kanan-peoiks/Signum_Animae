package com.example.bookingservice.repository;

import com.example.bookingservice.model.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByArtistIdOrderBySlotStartAsc(Long artistId);

    List<AvailabilitySlot> findByArtistIdAndBookedFalseAndSlotStartAfterOrderBySlotStartAsc(
            Long artistId, LocalDateTime after);

    Optional<AvailabilitySlot> findByArtistIdAndSlotStart(Long artistId, LocalDateTime slotStart);
}
