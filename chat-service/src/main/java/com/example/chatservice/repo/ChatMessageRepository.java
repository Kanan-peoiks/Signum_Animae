package com.example.chatservice.repo;

import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    List<ChatMessage> findByChatRoomIdAndReadFalseAndSenderIdNot(Long chatRoomId, Long senderId);

    /** Used for the "Söhbətlər" nav badge: how many unread messages (from the OTHER side) are
     *  waiting across all of this user's rooms, in one query instead of one-per-room. */
    long countByChatRoomIdInAndReadFalseAndSenderIdNot(List<Long> chatRoomIds, Long senderId);

    /** Usta analitika paneli üçün - bu ustanın göndərdiyi bütün OFFER mesajları
     *  (otaqlarından asılı olmayaraq), qəbul/rədd nisbətini hesablamaq üçün. */
    List<ChatMessage> findByChatRoomIdInAndSenderIdAndMessageType(List<Long> chatRoomIds, Long senderId, MessageType messageType);
}
