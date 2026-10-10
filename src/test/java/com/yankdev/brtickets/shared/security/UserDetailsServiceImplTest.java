package com.yankdev.brtickets.shared.security;

import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private UserModel user;

    @BeforeEach
    void setUp() {
        user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setEmail("yan@brtickets.com");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        user.setActive(true);
    }

    @Test
    @DisplayName("loads the user with the email as username and the stored hash")
    void loadsUserByEmail() {
        when(userRepository.findByEmail("yan@brtickets.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("yan@brtickets.com");

        assertThat(details.getUsername()).isEqualTo("yan@brtickets.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
    }

    @Test
    @DisplayName("maps the USER role to the ROLE_USER authority")
    void mapsUserRoleToAuthority() {
        when(userRepository.findByEmail("yan@brtickets.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("yan@brtickets.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("maps the ADMIN role to the ROLE_ADMIN authority")
    void mapsAdminRoleToAuthority() {
        user.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail("yan@brtickets.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("yan@brtickets.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("fails when no user has that email")
    void failsWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("ghost@brtickets.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@brtickets.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found by email");
    }
}
