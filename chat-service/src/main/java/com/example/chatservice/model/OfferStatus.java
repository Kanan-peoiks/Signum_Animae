package com.example.chatservice.model;

/** Only meaningful on ChatMessage rows where messageType == OFFER. */
public enum OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
