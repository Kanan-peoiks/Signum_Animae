package com.example.chatservice.exception;

public class NotRoomParticipantException extends RuntimeException {
    public NotRoomParticipantException(String message) {
        super(message);
    }
}
