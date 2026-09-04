package com.example.bookingservice.service;

import com.example.bookingservice.dto.AvailabilitySlotRequest;
import com.example.bookingservice.dto.AvailabilitySlotResponse;
import com.example.bookingservice.exception.AvailabilitySlotNotFoundException;
import com.example.bookingservice.exception.AvailabilityOwnershipException;
import com.example.bookingservice.model.AvailabilitySlot;
import com.example.bookingservice.repository.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Ustanın uyğunluq təqvimi - məlumat xarakterlidir, sifariş yaradılmasını
 *  məcburi məhdudlaşdırmır (BookingService.createBooking-a toxunmur). */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;

    public AvailabilitySlotResponse addSlot(AvailabilitySlotRequest request) {
        if (!request.getSlotEnd().isAfter(request.getSlotStart())) {
            throw new IllegalArgumentException("Bitmə vaxtı başlanğıcdan sonra olmalıdır.");
        }
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .artistId(request.getArtistId())
                .slotStart(request.getSlotStart())
                .slotEnd(request.getSlotEnd())
                .build();
        return mapToResponse(availabilitySlotRepository.save(slot));
    }

    /** Ustanın öz idarəetmə görünüşü - keçmiş və dolu olanlar da daxil, hamısı. */
    public List<AvailabilitySlotResponse> getSlotsForArtist(Long artistId) {
        return availabilitySlotRepository.findByArtistIdOrderBySlotStartAsc(artistId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Müştəriyə göstərilən ictimai siyahı - yalnız gələcək və boş pəncərələr. */
    public List<AvailabilitySlotResponse> getPublicSlots(Long artistId) {
        return availabilitySlotRepository
                .findByArtistIdAndBookedFalseAndSlotStartAfterOrderBySlotStartAsc(artistId, LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AvailabilitySlotResponse setBooked(Long slotId, Long callerArtistId, boolean booked) {
        AvailabilitySlot slot = findOrThrow(slotId);
        if (!slot.getArtistId().equals(callerArtistId)) {
            throw new AvailabilityOwnershipException("Bu pəncərə sizə aid deyil.");
        }
        slot.setBooked(booked);
        return mapToResponse(availabilitySlotRepository.save(slot));
    }

    public void deleteSlot(Long slotId, Long callerArtistId) {
        AvailabilitySlot slot = findOrThrow(slotId);
        if (!slot.getArtistId().equals(callerArtistId)) {
            throw new AvailabilityOwnershipException("Bu pəncərə sizə aid deyil.");
        }
        availabilitySlotRepository.delete(slot);
    }

    private AvailabilitySlot findOrThrow(Long id) {
        return availabilitySlotRepository.findById(id)
                .orElseThrow(() -> new AvailabilitySlotNotFoundException("Uyğunluq pəncərəsi tapılmadı! ID: " + id));
    }

    private AvailabilitySlotResponse mapToResponse(AvailabilitySlot slot) {
        return AvailabilitySlotResponse.builder()
                .id(slot.getId())
                .artistId(slot.getArtistId())
                .slotStart(slot.getSlotStart())
                .slotEnd(slot.getSlotEnd())
                .booked(slot.isBooked())
                .build();
    }
}
