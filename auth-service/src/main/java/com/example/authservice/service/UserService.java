package com.example.authservice.service;

import com.example.authservice.dto.InternalUserContactDto;
import com.example.authservice.dto.InternalUserSummaryDto;
import com.example.authservice.dto.UpdateUserProfileRequest;
import com.example.authservice.dto.UserProfileDto;
import com.example.authservice.exception.UserNotFoundException;
import com.example.authservice.model.User;
import com.example.authservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Plain-account self-service: editing name/city/avatar after registration.
 * This was a real gap before - register/login existed but nothing let a
 * customer or artist correct their own basic info afterwards.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;

    /**
     * @param self whether the caller IS this user (id == the verified caller id from
     *             X-User-Id). Anyone can look another user's basic profile up (names/
     *             avatars are shown all over the app), but email is only ever returned
     *             to its owner - see mapToDto.
     */
    public UserProfileDto getUser(Long id, boolean self) {
        return mapToDto(findOrThrow(id), self);
    }

    public InternalUserSummaryDto getInternalSummary(Long id) {
        User user = findOrThrow(id);
        return InternalUserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .build();
    }

    public InternalUserContactDto getInternalContact(Long id) {
        User user = findOrThrow(id);
        return InternalUserContactDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    public UserProfileDto updateUser(Long id, UpdateUserProfileRequest request) {
        User user = findOrThrow(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        return mapToDto(userRepository.save(user), true);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı! ID: " + id));
    }

    private UserProfileDto mapToDto(User user, boolean includeEmail) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(includeEmail ? user.getEmail() : null)
                .fullName(user.getFullName())
                .role(user.getRole())
                .city(user.getCity())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
