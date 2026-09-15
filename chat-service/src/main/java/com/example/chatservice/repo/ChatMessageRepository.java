package com.example.chatservice.repo;

import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Page<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);
    List<ChatMessage> findByChatRoomIdAndReadFalseAndSenderIdNot(Long chatRoomId, Long senderId);

    long countByChatRoomIdInAndReadFalseAndSenderIdNot(List<Long> chatRoomIds, Long senderId);

    List<ChatMessage> findByChatRoomIdInAndSenderIdAndMessageType(List<Long> chatRoomIds, Long senderId, MessageType messageType);
}
