package com.example.authservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** notification-service-in POST /api/v1/notifications/send gövdəsi.
 *  Email ünvanı göndərilmir - notification-service onu özü auth-service-dən soruşur. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String title;
    private String message;
    private boolean sendEmail;
}
