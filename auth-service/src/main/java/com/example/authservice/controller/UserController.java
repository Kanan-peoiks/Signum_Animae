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


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id, true));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserProfileDto> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }


    @GetMapping("/internal/{id}")
    public ResponseEntity<InternalUserSummaryDto> getUserSummaryInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getInternalSummary(id));
    }

    @GetMapping("/internal/{id}/contact")
    public ResponseEntity<InternalUserContactDto> getUserContactInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getInternalContact(id));
    }
}
