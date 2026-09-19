package com.example.authservice.service;

import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.model.Role;
import com.example.authservice.repo.ArtistProfileRepository;
import com.example.authservice.repo.UserRepo;
import com.example.authservice.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepo userRepository;
    @Mock private ArtistProfileRepository artistProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AccountTokenService accountTokenService;

    private AuthService authService() {
        return new AuthService(userRepository, artistProfileRepository,
                passwordEncoder, jwtUtil, accountTokenService);
    }

    private RegisterRequest request(Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("kimsə@test.com");
        request.setPassword("Demo12345");
        request.setFullName("Kimsə Kimsəzadə");
        request.setRole(role);
        return request;
    }

    /* Qeydiyyat endpoint-i hamıya açıqdır və rolu sorğudan götürür - ADMIN
       qəbul edilsəydi, istənilən kəs özünə admin paneli aça bilərdi. */
    @Test
    void register_rejectsAdminRole_andSavesNothing() {
        assertThatThrownBy(() -> authService().register(request(Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
        verify(artistProfileRepository, never()).save(any());
    }
}
