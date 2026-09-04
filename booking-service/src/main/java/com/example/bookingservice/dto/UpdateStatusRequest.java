package com.example.bookingservice.dto;

import com.example.bookingservice.model.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "status tələb olunur")
    private BookingStatus status;
}
