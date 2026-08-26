package com.example.aiservice.dto;

import lombok.Data;

@Data
public class TattooIdeaRequest {
    private String userPrompt;
    private String preferredStyle;
}