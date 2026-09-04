package com.example.chatservice.exception;

import com.example.chatservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> build(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, status.value()));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFound(ChatRoomNotFoundException ex) {
        return build(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidOfferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOffer(InvalidOfferException ex) {
        return build(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NotRoomParticipantException.class)
    public ResponseEntity<ErrorResponse> handleNotParticipant(NotRoomParticipantException ex) {
        return build(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BookingCancelledException.class)
    public ResponseEntity<ErrorResponse> handleBookingCancelled(BookingCancelledException ex) {
        return build(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Göndərilən məlumat düzgün deyil.");
        return build(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return build("Sistem xətası: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
