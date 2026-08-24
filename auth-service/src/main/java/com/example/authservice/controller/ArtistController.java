package com.example.authservice.controller;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping("/public/search")
    public ResponseEntity<List<ArtistProfileDto>> searchArtists(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String minRating) {

        Double minRatingVal = (minRating != null && !minRating.isBlank()) ? Double.parseDouble(minRating) : null;
        return ResponseEntity.ok(artistService.searchArtists(city, style, minRatingVal));
    }
}