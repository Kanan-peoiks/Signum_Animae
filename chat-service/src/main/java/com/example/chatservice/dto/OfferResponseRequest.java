package com.example.chatservice.dto;

import lombok.Data;

@Data
public class OfferResponseRequest {
    private Long userId;
    private boolean accept;
}
