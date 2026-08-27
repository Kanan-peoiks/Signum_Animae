package com.example.notificationservice.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String userEmail;
    private String title;
    private String message;
    private boolean sendEmail;
}