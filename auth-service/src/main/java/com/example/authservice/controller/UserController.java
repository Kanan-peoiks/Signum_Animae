package com.example.authservice.controller;

import com.example.authservice.dto.InternalUserContactDto;
import com.example.authservice.dto.InternalUserSummaryDto;
import com.example.authservice.dto.UpdateUserProfileRequest;
import com.example.authservice.dto.UserProfileDto;
import com.example.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * NOTE: the gateway already had a "user-service" route pointing /api/v1/users/**
 * at this exact service/port - it was just waiting for a controller. See
 * gateway-service/application.yaml.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Anyone logged in can look someone else up (needed to show names/avatars all over
     *  the app), but only the profile's OWNER gets their own email back in the response -
     *  see UserService.getUser. */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id, true));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserProfileDto> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Internal, service-to-service only (booking-service uses this to decide whether a
     * viewer gets a past-tattoo artist's full name or just initials - see
     * booking-service's completed-summary endpoint). Guarded by TrustedRequestFilter's
     * "/internal/" rule, not by a user identity - there is no end-user token in a
     * server-to-server call.
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<InternalUserSummaryDto> getUserSummaryInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getInternalSummary(id));
    }

    /**
     * Internal, service-to-service only. notification-service calls this to find out
     * who to actually email instead of trusting a frontend-supplied address - see the
     * comment on InternalUserContactDto.
     */
    @GetMapping("/internal/{id}/contact")
    public ResponseEntity<InternalUserContactDto> getUserContactInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getInternalContact(id));
    }
}
