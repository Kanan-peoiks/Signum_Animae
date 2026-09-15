package com.example.bookingservice.controller;

import com.example.bookingservice.dto.PlatformBookingStatsDto;
import com.example.bookingservice.service.AdminStatsService;
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

    @GetMapping("/bookings")
    public ResponseEntity<PlatformBookingStatsDto> bookingStats() {
        return ResponseEntity.ok(adminStatsService.bookingStats());
    }
}
