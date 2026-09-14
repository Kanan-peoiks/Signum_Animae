package com.example.notificationservice.repo;

import com.example.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Səhifələnmiş siyahı. Sıralama Pageable-dən gəlir (sabit sıra olmadan
     *  səhifələr arasında sətir təkrarlana və ya itə bilər). */
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    /** Yan paneldəki nişan üçün. Siyahı səhifələndiyi üçün oxunmamışları
     *  frontend-də saymaq artıq mümkün deyil - yalnız ilk səhifəni görər. */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnread(@Param("userId") Long userId);
}
