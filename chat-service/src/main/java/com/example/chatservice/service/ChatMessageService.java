package com.example.chatservice.service;

import com.example.chatservice.dto.ChatMessageRequest;
import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(ChatMessageRequest request) {
        ChatMessage message = ChatMessage.builder()
                .senderId(request.getSenderId())
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .build();

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(Long user1, Long user2) {
        return chatMessageRepository.findChatHistory(user1, user2);
    }
}