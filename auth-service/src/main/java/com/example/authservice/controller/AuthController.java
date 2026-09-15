package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.EmailRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.MessageResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse(
                "Əgər bu email sistemdə qeydiyyatdadırsa, bərpa linki göndərildi."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Şifrə yeniləndi. İndi yeni şifrə ilə daxil ola bilərsən."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Email ünvanı təsdiqləndi."));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<MessageResponse> sendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(new MessageResponse(
                "Əgər bu email təsdiqlənməyibsə, təsdiq linki göndərildi."));
    }
}
