package com.example.chatservice.exception;

public class BookingCancelledException extends RuntimeException {
    public BookingCancelledException(String message) {
        super(message);
    }
}
