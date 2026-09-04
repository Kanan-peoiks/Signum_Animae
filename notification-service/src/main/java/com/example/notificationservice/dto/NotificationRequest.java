package com.example.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {
    @NotNull(message = "userId tələb olunur")
    private Long userId;

    @NotBlank(message = "title boş ola bilməz")
    private String title;

    @NotBlank(message = "message boş ola bilməz")
    private String message;

    private boolean sendEmail;
}
