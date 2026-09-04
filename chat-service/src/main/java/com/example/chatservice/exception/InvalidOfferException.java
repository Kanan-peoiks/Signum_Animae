package com.example.chatservice.exception;

public class InvalidOfferException extends RuntimeException {
    public InvalidOfferException(String message) {
        super(message);
    }
}
