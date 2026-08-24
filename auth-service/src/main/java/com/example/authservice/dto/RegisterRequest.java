package com.example.authservice.dto;

import com.example.authservice.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private Role role;
    private String city;
}
