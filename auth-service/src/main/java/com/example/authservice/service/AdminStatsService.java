package com.example.authservice.service;

import com.example.authservice.dto.PlatformUserStatsDto;
import com.example.authservice.model.Role;
import com.example.authservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int TOP_CITY_LIMIT = 5;

    private final UserRepo userRepository;

    @Transactional(readOnly = true)
    public PlatformUserStatsDto userStats() {
        LocalDateTime now = LocalDateTime.now();

        List<PlatformUserStatsDto.CityCount> topCities =
                userRepository.countUsersByCity(PageRequest.of(0, TOP_CITY_LIMIT)).stream()
                        .map(row -> PlatformUserStatsDto.CityCount.builder()
                                .city((String) row[0])
                                .count(((Number) row[1]).longValue())
                                .build())
                        .collect(Collectors.toList());

        return PlatformUserStatsDto.builder()
                .totalUsers(userRepository.count())
                .customers(userRepository.countByRole(Role.CUSTOMER))
                .artists(userRepository.countByRole(Role.ARTIST))
                .admins(userRepository.countByRole(Role.ADMIN))
                .bannedUsers(userRepository.countByBannedTrue())
                .newLast7Days(userRepository.countByCreatedAtAfter(now.minusDays(7)))
                .newLast30Days(userRepository.countByCreatedAtAfter(now.minusDays(30)))
                .topCities(topCities)
                .build();
    }
}
