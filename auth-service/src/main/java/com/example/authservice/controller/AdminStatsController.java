package com.example.authservice.controller;

import com.example.authservice.dto.PlatformUserStatsDto;
import com.example.authservice.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/users")
    public ResponseEntity<PlatformUserStatsDto> userStats() {
        return ResponseEntity.ok(adminStatsService.userStats());
    }
}
