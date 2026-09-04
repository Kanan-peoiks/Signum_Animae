package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewReplyRequest {
    /** Cavab verən ustanın id-si - review.artistId ilə üst-üstə düşməlidir, əks halda 403. */
    @NotNull(message = "artistId tələb olunur")
    private Long artistId;

    @NotBlank(message = "Cavab mətni boş ola bilməz")
    @Size(max = 1000, message = "Cavab 1000 simvoldan uzun ola bilməz")
    private String reply;
}
