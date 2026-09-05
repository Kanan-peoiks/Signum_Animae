package com.example.authservice.controller;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Only reachable through the gateway's /api/v1/admin/** route, which is gated to a
 *  verified ADMIN-role JWT there - see gateway-service SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<AdminUserResponse> setBanned(@PathVariable Long id, @RequestParam boolean banned) {
        return ResponseEntity.ok(adminService.setBanned(id, banned));
    }
}
