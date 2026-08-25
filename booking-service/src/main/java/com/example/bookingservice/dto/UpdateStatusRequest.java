package com.example.bookingservice.dto;

import com.example.bookingservice.model.BookingStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private BookingStatus status;
}