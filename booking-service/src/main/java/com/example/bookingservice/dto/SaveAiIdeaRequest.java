package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveAiIdeaRequest {
    @NotNull(message = "customerId tələb olunur")
    private Long customerId;

    @NotBlank(message = "prompt boş ola bilməz")
    @Size(max = 2000, message = "prompt 2000 simvoldan uzun ola bilməz")
    private String prompt;

    @Size(max = 100)
    private String style;

    @NotBlank(message = "aiRecommendation boş ola bilməz")
    private String aiRecommendation;
}
