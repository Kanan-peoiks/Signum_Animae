package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAiIdeaResponse {
    private Long id;
    private Long customerId;
    private String prompt;
    private String style;
    private String aiRecommendation;
    private Long bookingId;
    private LocalDateTime createdAt;
}
