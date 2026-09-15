package com.example.authservice.service;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.exception.UserNotFoundException;
import com.example.authservice.model.User;
import com.example.authservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepo userRepository;

    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToResponse);
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
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
