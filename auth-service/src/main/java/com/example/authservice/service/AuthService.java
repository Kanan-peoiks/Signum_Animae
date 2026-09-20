package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.EmailRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.event.UserRegisteredEvent;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.exception.UserBannedException;
import com.example.authservice.exception.UserNotFoundException;
import com.example.authservice.model.ArtistProfile;
import com.example.authservice.model.AuthToken;
import com.example.authservice.model.AuthTokenType;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.repo.ArtistProfileRepository;
import com.example.authservice.repo.UserRepo;
import com.example.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountTokenService accountTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        /* Rol sorğudan gəlir və qeydiyyat endpoint-i hamıya açıqdır - ADMIN
           bağlanmasa, istənilən kəs özünə admin hesabı aça bilər. */
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin hesabı qeydiyyatdan yaradıla bilməz.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Bu email (" + request.getEmail() + ") artıq qeydiyyatdan keçib!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .city(request.getCity())
                .build();

        userRepository.save(user);

        if (user.getRole() == Role.ARTIST) {
            ArtistProfile profile = ArtistProfile.builder()
                    .user(user)
                    .ratingAvg(0.0)
                    .ratingCount(0)
                    .build();
            artistProfileRepository.save(profile);
        }

        /* Məktub yalnız tranzaksiya bağlandıqdan sonra göndərilir: notification-service
           e-poçtu öyrənmək üçün auth-service-ə geri müraciət edir və hələ commit olunmamış
           istifadəçini tapa bilmirdi (404) - nəticədə təsdiq məktubu heç vaxt getmirdi. */
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Daxil edilən şifrə yanlışdır!");
        }
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new UserBannedException("Hesabınız bloklanıb, daxil ola bilməzsiniz.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole());
    }

    public void forgotPassword(EmailRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user ->
                accountTokenService.sendPasswordResetEmail(
                        user.getId(), accountTokenService.issue(user.getId(), AuthTokenType.PASSWORD_RESET)));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        AuthToken token = accountTokenService.consume(request.getToken(), AuthTokenType.PASSWORD_RESET);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı!"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AuthToken token = accountTokenService.consume(rawToken, AuthTokenType.EMAIL_VERIFICATION);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı!"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void resendVerification(EmailRequest request) {
        userRepository.findByEmail(request.getEmail())
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> accountTokenService.sendVerificationEmail(
                        user.getId(), accountTokenService.issue(user.getId(), AuthTokenType.EMAIL_VERIFICATION)));
    }
}
