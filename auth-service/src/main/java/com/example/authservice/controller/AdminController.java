package com.example.authservice.controller;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.dto.PageParams;
import com.example.authservice.dto.PageResponse;
import com.example.authservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Only reachable through the gateway's /api/v1/admin/** route, which is gated to a
 *  verified ADMIN-role JWT there - see gateway-service SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** Səhifələnmişdir: ?page=0&size=20, cavab PageResponse ("content" içində). */
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // createdAt köhnə sətirlərdə null ola bilər, ona görə id DESC tək başına da
        // təkrarsız, sabit sıra verir.
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "id");
        return ResponseEntity.ok(PageResponse.from(adminService.listUsers(PageParams.of(page, size, newestFirst))));
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<AdminUserResponse> setBanned(@PathVariable Long id, @RequestParam boolean banned) {
        return ResponseEntity.ok(adminService.setBanned(id, banned));
    }
}
