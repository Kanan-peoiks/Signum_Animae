package com.example.aiservice.controller;

import com.example.aiservice.dto.TattooIdeaRequest;
import com.example.aiservice.dto.TattooIdeaResponse;
import com.example.aiservice.service.TattooAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final TattooAiService tattooAiService;

    @PostMapping("/generate-idea")
    public ResponseEntity<TattooIdeaResponse> generateIdea(@jakarta.validation.Valid @RequestBody TattooIdeaRequest request) {
        return ResponseEntity.ok(tattooAiService.generateTattooIdea(request));
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TattooIdeaResponse> analyzeImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "prompt", required = false) String prompt) {
        return ResponseEntity.ok(tattooAiService.analyzeTattooImage(image, prompt));
    }
}
