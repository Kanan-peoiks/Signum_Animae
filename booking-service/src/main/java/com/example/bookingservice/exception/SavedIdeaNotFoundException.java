package com.example.bookingservice.exception;

public class SavedIdeaNotFoundException extends RuntimeException {
    public SavedIdeaNotFoundException(String message) {
        super(message);
    }
}
