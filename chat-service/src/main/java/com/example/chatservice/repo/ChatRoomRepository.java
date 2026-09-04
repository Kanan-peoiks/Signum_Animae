package com.example.chatservice.repo;

import com.example.chatservice.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBookingId(Long bookingId);
    List<ChatRoom> findByCustomerId(Long customerId);
    List<ChatRoom> findByArtistId(Long artistId);

    /** All rooms this user is part of, regardless of whether they're the customer or the artist in it. */
    List<ChatRoom> findByCustomerIdOrArtistId(Long customerId, Long artistId);
}
