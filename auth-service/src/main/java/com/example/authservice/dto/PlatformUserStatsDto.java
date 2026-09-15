package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUserStatsDto {

    private long totalUsers;
    private long customers;
    private long artists;
    private long admins;
    private long bannedUsers;
    private long newLast7Days;
    private long newLast30Days;
    private List<CityCount> topCities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityCount {
        private String city;
        private long count;
    }
}
