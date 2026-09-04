package com.example.chatservice.dto;

import com.example.chatservice.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    /** Artıq etibar edilmir - göndərən həmişə doğrulanmış çağırandan (REST üçün X-User-Id,
     *  WS üçün handshake-də yoxlanan JWT) götürülür, bax ChatMessageService.saveMessage. */
    private Long senderId;

    @NotBlank(message = "content boş ola bilməz")
    @Size(max = 4000)
    private String content;

    private MessageType messageType;

    @Positive(message = "amount müsbət olmalıdır")
    private Double amount;
}
