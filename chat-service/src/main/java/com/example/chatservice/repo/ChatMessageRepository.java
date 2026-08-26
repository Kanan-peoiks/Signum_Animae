package com.example.chatservice.repo;

import com.example.chatservice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT c FROM ChatMessage c WHERE " +
            "(c.senderId = :user1 AND c.recipientId = :user2) OR " +
            "(c.senderId = :user2 AND c.recipientId = :user1) " +
            "ORDER BY c.timestamp ASC")
    List<ChatMessage> findChatHistory(@Param("user1") Long user1, @Param("user2") Long user2);
}