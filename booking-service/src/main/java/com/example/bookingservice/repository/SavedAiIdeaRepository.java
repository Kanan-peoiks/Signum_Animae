package com.example.bookingservice.repository;

import com.example.bookingservice.model.SavedAiIdea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedAiIdeaRepository extends JpaRepository<SavedAiIdea, Long> {
    List<SavedAiIdea> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
