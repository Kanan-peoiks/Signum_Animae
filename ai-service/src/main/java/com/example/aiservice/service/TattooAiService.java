package com.example.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiservice.dto.TattooIdeaRequest;
import com.example.aiservice.dto.TattooIdeaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    private static final Map<String, Object> GENERATION_CONFIG = Map.of(
            "thinkingConfig", Map.of("thinkingBudget", 0)
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TattooAiService() {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public TattooIdeaResponse generateTattooIdea(TattooIdeaRequest request) {
        String systemPrompt = String.format(
                "You are a professional tattoo designer and consultant with deep expertise in tattoo art styles, placement, symbolism, and pricing in the Azerbaijani market (mainly Baku-based professional studios and artists).\n\n" +
                        "CRITICAL: Regardless of the language of the input, you must ALWAYS respond in Azerbaijani (Azərbaycan dili). Never respond in English, Russian, or any other language.\n\n" +
                        "IMPORTANT — INPUT VALIDATION (check this FIRST, before anything else):\n" +
                        "- If the user's input contains profanity, insults, offensive language, or abusive content → respond ONLY with a short, polite message in Azerbaijani asking the user to rephrase respectfully. Do NOT generate any tattoo concept. Do NOT repeat or quote the offensive words.\n" +
                        "- If the user's input is irrelevant to tattoo design (e.g. random topics, unrelated questions, spam, nonsense text) → respond ONLY with a short, polite message in Azerbaijani explaining that you can only help with tattoo idea consultations, and ask them to describe a tattoo idea instead. Do NOT generate any tattoo concept.\n" +
                        "- Only if the input is a genuine, relevant tattoo idea or a reasonable description related to tattoos, proceed with the full structured consultation below.\n\n" +
                        "User's tattoo idea: \"%s\"\n" +
                        "Preferred style: \"%s\"\n\n" +
                        "If the input passed validation, provide a structured, professional consultation covering the following sections, using Markdown formatting with clear headers (###) and bold (**text**) for key terms:\n\n" +
                        "1. **Konsept təsviri** — Briefly describe what the tattoo concept looks like, its artistic style (e.g. fine-line, realism, minimalist, anime/illustrative, blackwork, watercolor, etc.), and what makes it visually distinctive.\n\n" +
                        "2. **Yerləşmə məsləhəti** — Recommend 1-2 best body placements for this design, explaining why (based on shape, size, visibility, pain tolerance area).\n\n" +
                        "3. **Elementlərin mənası** — If the idea includes symbolic elements (animals, objects, colors), briefly explain their common symbolic meaning.\n\n" +
                        "4. **Təxmini Qiymət Aralığı** — Provide a realistic price estimate broken into at least 2 size tiers, each with:\n" +
                        "   - Approximate size in cm\n" +
                        "   - Example placement for that size\n" +
                        "   - Price range in AZN\n" +
                        "   - Note if multiple sessions may be needed for larger sizes\n\n" +
                        "5. **Nəzərə almalı vacib məqamlar** — Include practical considerations such as:\n" +
                        "   - The required skill level of the artist (e.g. fine-line, shading, small detail work)\n" +
                        "   - Advice to choose an artist with a matching portfolio style\n" +
                        "   - Any factor that could affect final price (color use, detail density, placement difficulty)\n\n" +
                        "Keep the tone professional, concise, and helpful — like an experienced tattoo consultant giving honest advice, not generic marketing language. Avoid unnecessary repetition. Total response should be well-organized but not excessively long (aim for clarity over length).",
                request.getUserPrompt(),
                request.getPreferredStyle() != null ? request.getPreferredStyle() : "Ən uyğun stil"
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemPrompt)
                        ))
                ),
                "generationConfig", GENERATION_CONFIG
        );

        return callGeminiApi(requestBody);
    }

    public TattooIdeaResponse analyzeTattooImage(MultipartFile image, String prompt) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            String basePrompt =
                    "You are a professional tattoo artist and image analyst with expert-level visual perception of tattoo designs, sketches, and reference images.\n\n" +
                            "CRITICAL: Regardless of the language of the input, you must ALWAYS respond in Azerbaijani (Azərbaycan dili). Never respond in English, Russian, or any other language.\n\n" +
                            "IMPORTANT — INPUT VALIDATION (check this FIRST, before anything else):\n" +
                            "- If the uploaded image is NOT a tattoo, sketch, or tattoo-related design (e.g. it's an unrelated photo, random object, screenshot, or inappropriate/explicit content) → respond ONLY with a short, polite message in Azerbaijani explaining that you can only analyze tattoo designs or sketches, and ask them to upload a relevant image. Do NOT proceed with analysis.\n" +
                            "- If the user's additional question/text contains profanity, insults, or abusive language → respond ONLY with a short, polite message in Azerbaijani asking the user to rephrase respectfully. Do NOT proceed with analysis. Do NOT repeat or quote the offensive words.\n" +
                            "- If the user's additional question is irrelevant to tattoos (e.g. random unrelated topic) → politely note in Azerbaijani that you can only help with tattoo-related questions, then still proceed with the structured image analysis below if the image itself is valid.\n" +
                            "- Only if both the image and the question (if any) pass validation, proceed with the full structured analysis below.\n\n" +
                            "The user has uploaded a tattoo image or sketch for analysis.\n" +
                            "Additional question from user (if provided): \"%s\"\n\n" +
                            "If validation passes, carefully analyze the uploaded image and provide a structured, professional breakdown using Markdown formatting with clear headers (###) and bold (**text**) for key terms, covering:\n\n" +
                            "1. **Stil analizi** — Identify the artistic style of the design (e.g. fine-line, realism, blackwork, anime/illustrative, geometric, watercolor, traditional, etc.) and describe its defining visual characteristics.\n\n" +
                            "2. **Detal və mürəkkəblik səviyyəsi** — Assess the complexity of the linework, shading, and fine details (e.g. hair strands, scales, small textures) and what skill level would be required to execute it well.\n\n" +
                            "3. **Uyğun bədən hissəsi** — Recommend which body placement(s) would best suit this design's shape, size, and orientation, and explain why.\n\n" +
                            "4. **İnkişaf təklifləri** — Suggest how the design could be improved, adapted, or scaled (e.g. adding color accents, adjusting proportions, simplifying for smaller size, or expanding for larger placement).\n\n" +
                            "5. **Təxmini qiymət və vacib qeydlər** — If enough visual information is available, give a rough price estimate range in AZN based on estimated size and complexity, and note key factors (artist skill required, color use, session count) that could affect the final cost.\n\n" +
                            "If the user asked a specific relevant question, prioritize answering that question directly first, then continue with the structured analysis above where relevant.\n\n" +
                            "Keep the tone professional and precise, like an experienced tattoo artist reviewing a client's reference — avoid vague or generic statements.";

            String userPrompt = String.format(
                    basePrompt,
                    (prompt != null && !prompt.isBlank()) ? prompt : "Yoxdur"
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", userPrompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType,
                                            "data", base64Image
                                    ))
                            ))
                    ),
                    "generationConfig", GENERATION_CONFIG
            );

            return callGeminiApi(requestBody);

        } catch (Exception e) {
            return TattooIdeaResponse.builder()
                    .aiRecommendation("Şəkil oxunarkən xəta baş verdi: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Gemini-nin 503 (Service Unavailable) və 429 (Too Many Requests) cavabları
     * müvəqqətidir - server anlıq yüklüdür və ya dəqiqəlik kvota dolub. Tək bir
     * sorğuda uğursuz sayıb istifadəçiyə xəta göstərmək əvəzinə qısa fasilə ilə
     * bir neçə dəfə təkrar cəhd edirik; praktikada ikinci-üçüncü cəhd adətən keçir.
     * 4xx (məsələn səhv API açarı) təkrarlanmır - onu təkrar etmək mənasızdır.
     */
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MS = { 1200L, 2800L };

    private TattooIdeaResponse callGeminiApi(Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Exception lastError = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                String responseJson = webClient.post()
                        .uri(url)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode candidates = objectMapper.readTree(responseJson).path("candidates");

                if (candidates.isArray() && candidates.size() > 0) {
                    String aiText = candidates.get(0)
                            .path("content").path("parts").get(0).path("text").asText();
                    return TattooIdeaResponse.builder().aiRecommendation(aiText).build();
                }
                return TattooIdeaResponse.builder()
                        .aiRecommendation("Analiz baş tutmadı: server cavabında candidate tapılmadı.")
                        .build();

            } catch (WebClientResponseException ex) {
                lastError = ex;
                if (!isRetryable(ex.getStatusCode().value()) || attempt == MAX_ATTEMPTS - 1) {
                    break;
                }
                sleepQuietly(BACKOFF_MS[attempt]);

            } catch (Exception ex) {
                lastError = ex;
                break;
            }
        }

        return TattooIdeaResponse.builder()
                .aiRecommendation(describeFailure(lastError))
                .build();
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    private String describeFailure(Exception error) {
        if (error instanceof WebClientResponseException wex) {
            int status = wex.getStatusCode().value();
            if (status == 503 || status == 500 || status == 502 || status == 504) {
                return "AI xidməti hazırda cavab vermir (Google tərəfində müvəqqəti yüklənmə). "
                        + MAX_ATTEMPTS + " dəfə cəhd edildi. Bir neçə dəqiqədən sonra yenidən yoxla.";
            }
            if (status == 429) {
                return "AI sorğu limiti dolub (pulsuz paketdə gündəlik/dəqiqəlik hədd). "
                        + "Bir az gözlə və ya Google AI Studio-da limiti artır.";
            }
            if (status == 400 || status == 403) {
                return "AI sorğusu qəbul edilmədi - API açarı və ya model adı yanlış ola bilər.";
            }
            return "AI servisi " + status + " kodu ilə cavab verdi.";
        }
        return "AI servisi ilə əlaqə zamanı xəta baş verdi: "
                + (error != null ? error.getMessage() : "naməlum səbəb");
    }
}
