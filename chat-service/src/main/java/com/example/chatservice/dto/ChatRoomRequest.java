package com.example.chatservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRoomRequest {
    @NotNull(message = "customerId tələb olunur")
    private Long customerId;

    @NotNull(message = "artistId tələb olunur")
    private Long artistId;

    @NotNull(message = "bookingId tələb olunur")
    private Long bookingId;
}
