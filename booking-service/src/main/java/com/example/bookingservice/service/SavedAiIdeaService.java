package com.example.bookingservice.service;

import com.example.bookingservice.dto.SaveAiIdeaRequest;
import com.example.bookingservice.dto.SavedAiIdeaResponse;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.exception.SavedIdeaNotFoundException;
import com.example.bookingservice.exception.SavedIdeaOwnershipException;
import com.example.bookingservice.model.SavedAiIdea;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.SavedAiIdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** AI Studiyada alınan bir məsləhəti/analizi saxlamaq (bəyənmək) və istəyə görə
 *  mövcud bir sifarişə bağlamaq. ai-service tamamilə "stateless"dir (öz DB-si
 *  yoxdur), ona görə saxlanmış ideyalar burada, booking-service-in artıq mövcud
 *  olan Postgres bazasında saxlanılır - əlavə bir yeni verilənlər bazası deyil,
 *  sadəcə mövcud bazaya bir yeni cədvəl. */
@Service
@RequiredArgsConstructor
public class SavedAiIdeaService {

    private final SavedAiIdeaRepository savedAiIdeaRepository;
    private final BookingRepository bookingRepository;

    public SavedAiIdeaResponse saveIdea(SaveAiIdeaRequest request) {
        SavedAiIdea idea = SavedAiIdea.builder()
                .customerId(request.getCustomerId())
                .prompt(request.getPrompt())
                .style(request.getStyle())
                .aiRecommendation(request.getAiRecommendation())
                .build();
        return mapToResponse(savedAiIdeaRepository.save(idea));
    }

    public List<SavedAiIdeaResponse> getSavedIdeas(Long customerId) {
        return savedAiIdeaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SavedAiIdeaResponse linkToBooking(Long ideaId, Long callerId, Long bookingId) {
        SavedAiIdea idea = findOrThrow(ideaId);
        if (!idea.getCustomerId().equals(callerId)) {
            throw new SavedIdeaOwnershipException("Bu ideya sizə aid deyil.");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException("Bron tapılmadı! ID: " + bookingId);
        }
        idea.setBookingId(bookingId);
        return mapToResponse(savedAiIdeaRepository.save(idea));
    }

    public void deleteIdea(Long ideaId, Long callerId) {
        SavedAiIdea idea = findOrThrow(ideaId);
        if (!idea.getCustomerId().equals(callerId)) {
            throw new SavedIdeaOwnershipException("Bu ideya sizə aid deyil.");
        }
        savedAiIdeaRepository.delete(idea);
    }

    private SavedAiIdea findOrThrow(Long id) {
        return savedAiIdeaRepository.findById(id)
                .orElseThrow(() -> new SavedIdeaNotFoundException("Saxlanmış ideya tapılmadı! ID: " + id));
    }

    private SavedAiIdeaResponse mapToResponse(SavedAiIdea idea) {
        return SavedAiIdeaResponse.builder()
                .id(idea.getId())
                .customerId(idea.getCustomerId())
                .prompt(idea.getPrompt())
                .style(idea.getStyle())
                .aiRecommendation(idea.getAiRecommendation())
                .bookingId(idea.getBookingId())
                .createdAt(idea.getCreatedAt())
                .build();
    }
}
