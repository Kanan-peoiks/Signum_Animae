package com.example.bookingservice.exception;

/** Həmin vaxta ustanın aktiv sifarişi var. */
public class SlotAlreadyTakenException extends RuntimeException {
    public SlotAlreadyTakenException(String message) {
        super(message);
    }
}
