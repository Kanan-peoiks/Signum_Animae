package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sadə mətn cavabı - şifrə sıfırlama/email təsdiqi endpoint-ləri nə istifadəçi, nə də
 *  token qaytarmır, yalnız nəticəni bildirir. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
