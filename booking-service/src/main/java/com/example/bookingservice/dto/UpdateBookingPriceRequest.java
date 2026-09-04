package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateBookingPriceRequest {
    @NotNull(message = "estimatedPrice tələb olunur")
    @Positive(message = "Qiymət müsbət olmalıdır")
    private Double estimatedPrice;
}
