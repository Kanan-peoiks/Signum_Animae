package com.example.bookingservice.exception;

public class AvailabilitySlotNotFoundException extends RuntimeException {
    public AvailabilitySlotNotFoundException(String message) {
        super(message);
    }
}
