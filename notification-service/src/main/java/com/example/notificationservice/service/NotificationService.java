package com.example.notificationservice.service;

import com.example.notificationservice.client.AuthServiceClient;
import com.example.notificationservice.client.dto.InternalUserContactDto;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final AuthServiceClient authServiceClient;

    public Notification sendNotification(NotificationRequest request) {
        // The email address always comes from auth-service's own record for this user,
        // never from the request - a caller (or a compromised frontend) used to be able
        // to send NotificationRequest.userEmail directly, which meant it could ask this
        // service to email literally any address it wanted "as" a notification to
        // userId. Resolving it here also means the frontend no longer needs to fetch
        // another user's email into the browser at all (see api.js's old notifyQuietly).
        String resolvedEmail = resolveEmail(request.getUserId());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .userEmail(resolvedEmail)
                .title(request.getTitle())
                .message(request.getMessage())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        if (request.isSendEmail() && resolvedEmail != null && !resolvedEmail.isBlank()) {
            sendEmail(resolvedEmail, request.getTitle(), request.getMessage());
        }

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** @return false if the notification doesn't exist or doesn't belong to callerId -
     *  the controller turns that into 404/403 as appropriate; true if it was marked read. */
    public boolean markAsRead(Long id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    private String resolveEmail(Long userId) {
        try {
            InternalUserContactDto contact = authServiceClient.getUserContact(userId);
            return contact != null ? contact.getEmail() : null;
        } catch (Exception ex) {
            log.error("İstifadəçinin e-poçtu alınmadı (userId={}): {}", userId, ex.getMessage(), ex);
            return null;
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);
            mailSender.send(mailMessage);
        } catch (Exception e) {
            log.error("Email göndərilərkən xəta baş verdi (to={}): {}", to, e.getMessage(), e);
        }
    }
}
