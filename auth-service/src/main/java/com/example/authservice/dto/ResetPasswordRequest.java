package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Token boş ola bilməz.")
    private String token;

    @NotBlank(message = "Yeni şifrə boş ola bilməz.")
    @Size(min = 6, message = "Şifrə ən azı 6 simvol olmalıdır.")
    private String newPassword;
}
