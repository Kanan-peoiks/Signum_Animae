package com.example.authservice.service;

import com.example.authservice.dto.UpdateUserProfileRequest;
import com.example.authservice.dto.UserProfileDto;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the email-masking rule added alongside the cross-service authorization fixes:
 * GET /api/v1/users/{id} used to return the SAME full profile (including email) no
 * matter who asked - any logged-in user could read a stranger's email. Now only the
 * profile's own owner gets it back.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepository;

    private UserService userService;

    private void init() {
        userService = new UserService(userRepository);
    }

    private User sampleUser() {
        return User.builder()
                .id(7L).email("aygun@test.com").fullName("Aygün Məmmədova")
                .role(Role.CUSTOMER).city("Bakı").premium(false)
                .build();
    }

    @Test
    void getUser_returnsEmail_whenViewerIsTheOwner() {
        init();
        when(userRepository.findById(7L)).thenReturn(Optional.of(sampleUser()));

        UserProfileDto dto = userService.getUser(7L, true);

        assertThat(dto.getEmail()).isEqualTo("aygun@test.com");
        assertThat(dto.getFullName()).isEqualTo("Aygün Məmmədova"); // name is still public either way
    }

    @Test
    void getUser_hidesEmail_whenViewerIsSomeoneElse() {
        init();
        when(userRepository.findById(7L)).thenReturn(Optional.of(sampleUser()));

        UserProfileDto dto = userService.getUser(7L, false);

        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getFullName()).isEqualTo("Aygün Məmmədova");
    }

    @Test
    void updateUser_togglesPremiumFlag() {
        init();
        User user = sampleUser();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPremium(true);

        UserProfileDto dto = userService.updateUser(7L, request);

        assertThat(dto.isPremium()).isTrue();
    }

    @Test
    void updateUser_leavesFieldsUnchanged_whenRequestFieldsAreNull() {
        init();
        User user = sampleUser();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDto dto = userService.updateUser(7L, new UpdateUserProfileRequest());

        assertThat(dto.getFullName()).isEqualTo("Aygün Məmmədova");
        assertThat(dto.getCity()).isEqualTo("Bakı");
    }
}
