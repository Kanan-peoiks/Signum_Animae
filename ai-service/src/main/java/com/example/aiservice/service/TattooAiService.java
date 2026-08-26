package com.example.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiservice.dto.TattooIdeaRequest;
import com.example.aiservice.dto.TattooIdeaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class TattooAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-flash-latest}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TattooAiService() {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public TattooIdeaResponse generateTattooIdea(TattooIdeaRequest request) {
        String systemPrompt = String.format(
                "Sən peşəkar tatuirovka dizayneri və ustadısan. İstifadəçinin ideyası: '%s'. " +
                        "İstənilən stil: '%s'. Bu ideya əsasında detallı tatuirovka konsepti, yerləşmə məsləhəti və elementlərin mənası barədə Azərbaycan dilində qısa, dəqiq məsləhətlər ver.",
                request.getUserPrompt(),
                request.getPreferredStyle() != null ? request.getPreferredStyle() : "Ən uyğun stil"
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemPrompt)
                        ))
                )
        );

        return callGeminiApi(requestBody);
    }

    public TattooIdeaResponse analyzeTattooImage(MultipartFile image, String prompt) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            String userPrompt = (prompt != null && !prompt.isBlank())
                    ? prompt
                    : "Sən peşəkar tatuirovka ustadısan. Bu tatu şəklini/eskizini diqqətlə analiz et. Stilini, detallarını, bədənin hansı hissəsinə daha yaxşı yaraşacağını və necə inkişaf etdirilə biləcəyini Azərbaycan dilində izah et.";

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", userPrompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType,
                                            "data", base64Image
                                    ))
                            ))
                    )
            );

            return callGeminiApi(requestBody);

        } catch (Exception e) {
            return TattooIdeaResponse.builder()
                    .aiRecommendation("Şəkil oxunarkən xəta baş verdi: " + e.getMessage())
                    .build();
        }
    }

    private TattooIdeaResponse callGeminiApi(Map<String, Object> requestBody) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            String responseJson = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidate = root.path("candidates");

            if (candidate.isArray() && candidate.size() > 0) {
                String aiText = candidate.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                return TattooIdeaResponse.builder()
                        .aiRecommendation(aiText)
                        .build();
            } else {
                return TattooIdeaResponse.builder()
                        .aiRecommendation("Analiz baş tutmadı: server cavabında candidate tapılmadı.")
                        .build();
            }

        } catch (Exception e) {
            return TattooIdeaResponse.builder()
                    .aiRecommendation("AI servisi ilə əlaqə zamanı xəta baş verdi: " + e.getMessage())
                    .build();
        }
    }
}