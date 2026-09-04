package com.example.chatservice.dto;

import lombok.Data;

@Data
public class OfferResponseRequest {
    /** İstifadə olunmur - kim cavab verdiyi doğrulanmış çağırandan gəlir, bax
     *  ChatController/ChatMessageService.respondToOffer. */
    private Long userId;
    private boolean accept;
}
