package com.example.authservice.service;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.exception.UserNotFoundException;
import com.example.authservice.model.User;
import com.example.authservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Moderation-only operations. Reachable ONLY through /api/v1/admin/** - the gateway
 *  is what actually restricts this to callers with a verified ADMIN role in their JWT
 *  (see gateway-service's SecurityConfig); nothing in this service re-checks that,
 *  same trust boundary every other cross-service call in this project relies on. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepo userRepository;

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AdminUserResponse setBanned(Long userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı! ID: " + userId));
        user.setBanned(banned);
        return mapToResponse(userRepository.save(user));
    }

    private AdminUserResponse mapToResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .city(user.getCity())
                .banned(Boolean.TRUE.equals(user.getBanned()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
