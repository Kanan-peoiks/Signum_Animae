package com.example.authservice.service;

import com.example.authservice.event.UserRegisteredEvent;
import com.example.authservice.model.AuthTokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Təsdiq məktubu qeydiyyat tranzaksiyası bağlandıqdan SONRA göndərilir.
 * Əvvəl məktub tranzaksiyanın içindən göndərilirdi: notification-service
 * e-poçtu öyrənmək üçün auth-service-ə geri müraciət edir, hələ commit
 * olunmamış istifadəçini görmür (404) və məktub heç vaxt getmirdi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEmailListener {

    private final AccountTokenService accountTokenService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            accountTokenService.sendVerificationEmail(
                    event.userId(),
                    accountTokenService.issue(event.userId(), AuthTokenType.EMAIL_VERIFICATION));
        } catch (Exception ex) {
            // Məktub getməsə də qeydiyyat baş tutub - istifadəçini bloklamırıq
            log.error("Təsdiq məktubu göndərilmədi (userId={}): {}", event.userId(), ex.getMessage(), ex);
        }
    }
}
