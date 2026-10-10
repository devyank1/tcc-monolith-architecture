package com.yankdev.brtickets.shared.security;

import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticatedUserProvider userProvider;

    private UserModel user;

    @BeforeEach
    void setUp() {
        user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setEmail("yan@brtickets.com");
        user.setRole(UserRole.USER);
        user.setActive(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(email, "password"));
    }

    @Test
    @DisplayName("getCurrentUser returns the user behind the authentication")
    void returnsAuthenticatedUser() {
        authenticate("yan@brtickets.com");
        when(userRepository.findByEmail("yan@brtickets.com")).thenReturn(Optional.of(user));

        UserModel current = userProvider.getCurrentUser();

        assertThat(current.getEmail()).isEqualTo("yan@brtickets.com");
        assertThat(current.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("getCurrentUserId returns only the id of the authenticated user")
    void returnsAuthenticatedUserId() {
        authenticate("yan@brtickets.com");
        when(userRepository.findByEmail("yan@brtickets.com")).thenReturn(Optional.of(user));

        assertThat(userProvider.getCurrentUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("fails when the authenticated email has no user in the database")
    void failsWhenAuthenticatedUserIsUnknown() {
        authenticate("ghost@brtickets.com");
        when(userRepository.findByEmail("ghost@brtickets.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProvider.getCurrentUser())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Authenticated user not found");
    }

    @Test
    @DisplayName("fails with NullPointerException when there is no authentication at all")
    void failsWhenThereIsNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userProvider.getCurrentUser())
                .isInstanceOf(NullPointerException.class);
    }
}
