package com.example.bookingservice.exception;

/** Sifarişin vəziyyəti bu istiqamətdə dəyişdirilə bilməz. */
public class InvalidStatusChangeException extends RuntimeException {
    public InvalidStatusChangeException(String message) {
        super(message);
    }
}
