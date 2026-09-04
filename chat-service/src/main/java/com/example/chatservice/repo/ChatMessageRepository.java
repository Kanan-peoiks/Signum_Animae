package com.example.chatservice.repo;

import com.example.chatservice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    List<ChatMessage> findByChatRoomIdAndReadFalseAndSenderIdNot(Long chatRoomId, Long senderId);

    /** Used for the "Söhbətlər" nav badge: how many unread messages (from the OTHER side) are
     *  waiting across all of this user's rooms, in one query instead of one-per-room. */
    long countByChatRoomIdInAndReadFalseAndSenderIdNot(List<Long> chatRoomIds, Long senderId);
}
