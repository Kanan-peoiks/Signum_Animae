package com.example.notificationservice.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserContactDto {
    private Long id;
    private String email;
    private String fullName;
}
