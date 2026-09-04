package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "E-poçt tələb olunur")
    private String email;

    @NotBlank(message = "Şifrə tələb olunur")
    private String password;
}
