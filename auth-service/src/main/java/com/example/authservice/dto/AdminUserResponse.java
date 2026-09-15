package com.example.authservice.dto;

import com.example.authservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private String city;
    private boolean banned;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
}
