package com.example.authservice.dto;

import com.example.authservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Admin-only view of a user - unlike UserProfileDto, this always includes the email
 *  and the moderation-relevant "banned" flag, regardless of who's asking. Only ever
 *  returned from an endpoint gated to ADMIN at the gateway (see gateway SecurityConfig). */
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
    private LocalDateTime createdAt;
}
