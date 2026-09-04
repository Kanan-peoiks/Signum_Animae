package com.example.bookingservice.exception;

public class BookingNotCompletedException extends RuntimeException {
    public BookingNotCompletedException(String message) {
        super(message);
    }
}
