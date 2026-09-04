package com.example.authservice.dto;

import com.example.authservice.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "E-poçt tələb olunur")
    @Email(message = "E-poçt düzgün formatda deyil")
    private String email;

    @NotBlank(message = "Şifrə tələb olunur")
    @Size(min = 6, message = "Şifrə ən azı 6 simvol olmalıdır")
    private String password;

    @NotBlank(message = "Ad, soyad tələb olunur")
    @Size(max = 150)
    private String fullName;

    @NotNull(message = "Rol tələb olunur")
    private Role role;

    @Size(max = 150)
    private String city;
}
