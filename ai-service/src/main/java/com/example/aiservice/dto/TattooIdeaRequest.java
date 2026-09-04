package com.example.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TattooIdeaRequest {
    @NotBlank(message = "Təsvir boş ola bilməz")
    @Size(max = 2000, message = "Təsvir 2000 simvoldan uzun ola bilməz")
    private String userPrompt;

    @Size(max = 100)
    private String preferredStyle;
}
