package com.example.bookingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "bookingId tələb olunur")
    private Long bookingId;

    /** Artıq etibar edilmir - sahibliyi ReviewService callerId ilə (X-User-Id-dən) yoxlayır,
     *  bax ReviewController/ReviewService. Köhnə klientlərlə uyğunluq üçün sahə saxlanılıb. */
    private Long customerId;

    @NotNull(message = "Reytinq tələb olunur")
    @Min(value = 1, message = "Reytinq 1 ilə 5 arasında olmalıdır")
    @Max(value = 5, message = "Reytinq 1 ilə 5 arasında olmalıdır")
    private Integer rating;

    @Size(max = 1000, message = "Şərh 1000 simvoldan uzun ola bilməz")
    private String comment;
}
