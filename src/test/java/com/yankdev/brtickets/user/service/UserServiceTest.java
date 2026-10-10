package com.yankdev.brtickets.user.service;

import com.yankdev.brtickets.shared.exception.CpfAlreadyExistsException;
import com.yankdev.brtickets.shared.exception.EmailAlreadyExistsException;
import com.yankdev.brtickets.shared.exception.InvalidCredentialsException;
import com.yankdev.brtickets.shared.exception.UserIsNotActiveException;
import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder pwdEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<UserModel> userCaptor;

    private UserRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new UserRequestDTO();
        request.setFirstName("Yan");
        request.setLastName("Carlos");
        request.setEmail("yan@brtickets.com");
        request.setPassword("plain-password");
        request.setCpf("12345678901");
        request.setPhone("11999999999");
        request.setBirthday(LocalDate.of(2000, 1, 15));
    }

    private UserModel existingUser() {
        UserModel user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setFirstName("Old");
        user.setLastName("Name");
        user.setEmail("old@brtickets.com");
        user.setPasswordHash("hashed-old-password");
        user.setCpf("98765432100");
        user.setPhone("11888888888");
        user.setBirthday(LocalDate.of(1995, 6, 10));
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        return user;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates user with role USER, active true and createdAt filled")
        void registersNewUser() {
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByCpf(request.getCpf())).thenReturn(false);
            when(pwdEncoder.encode("plain-password")).thenReturn("hashed-password");
            when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));

            UserResponseDTO response = userService.register(request);

            assertThat(response.getEmail()).isEqualTo("yan@brtickets.com");
            assertThat(response.getFirstName()).isEqualTo("Yan");
            assertThat(response.getRole()).isEqualTo(UserRole.USER);
            assertThat(response.isActive()).isTrue();
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("stores the password hashed, never the raw value")
        void hashesPasswordBeforeSaving() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByCpf(anyString())).thenReturn(false);
            when(pwdEncoder.encode("plain-password")).thenReturn("hashed-password");
            when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));

            userService.register(request);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash())
                    .isEqualTo("hashed-password")
                    .isNotEqualTo("plain-password");
        }

        @Test
        @DisplayName("rejects duplicated email and never saves")
        void rejectsDuplicatedEmail() {
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email already exists.");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects duplicated CPF and never saves")
        void rejectsDuplicatedCpf() {
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByCpf(request.getCpf())).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(CpfAlreadyExistsException.class)
                    .hasMessage("CPF already exists.");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns the user data with a JWT token")
        void loginsWithValidCredentials() {
            UserModel user = existingUser();
            request.setEmail(user.getEmail());
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(pwdEncoder.matches("plain-password", "hashed-old-password")).thenReturn(true);
            when(jwtUtils.generateToken(user)).thenReturn("jwt-token");

            UserResponseDTO response = userService.login(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            verify(jwtUtils).generateToken(user);
        }

        @Test
        @DisplayName("rejects unknown email without generating a token")
        void rejectsUnknownEmail() {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid credentials.");

            verify(jwtUtils, never()).generateToken(any());
        }

        @Test
        @DisplayName("rejects wrong password without generating a token")
        void rejectsWrongPassword() {
            UserModel user = existingUser();
            request.setEmail(user.getEmail());
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(pwdEncoder.matches("plain-password", "hashed-old-password")).thenReturn(false);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid credentials.");

            verify(jwtUtils, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("findUser / findAllUsers")
    class Queries {

        @Test
        @DisplayName("returns the user found by id")
        void findsUserById() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

            UserResponseDTO response = userService.findUser(user.getUserId());

            assertThat(response.getUserId()).isEqualTo(user.getUserId());
            assertThat(response.getEmail()).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("fails when the id does not exist")
        void failsWhenUserIdDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUser(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found by USER ID.");
        }

        @Test
        @DisplayName("maps every user to a DTO")
        void findsAllUsers() {
            UserModel first = existingUser();
            UserModel second = existingUser();
            second.setEmail("second@brtickets.com");
            when(userRepository.findAll()).thenReturn(List.of(first, second));

            List<UserResponseDTO> response = userService.findAllUsers();

            assertThat(response).hasSize(2)
                    .extracting(UserResponseDTO::getEmail)
                    .containsExactly("old@brtickets.com", "second@brtickets.com");
        }

        @Test
        @DisplayName("returns an empty list when there is no user")
        void findsAllUsersWhenEmpty() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.findAllUsers()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("updates only the fields sent in the request")
        void updatesOnlyNonNullFields() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));

            UserRequestDTO partial = new UserRequestDTO();
            partial.setFirstName("NewName");
            partial.setPhone("11777777777");

            UserResponseDTO response = userService.updateUser(user.getUserId(), partial);

            assertThat(response.getFirstName()).isEqualTo("NewName");
            assertThat(response.getPhone()).isEqualTo("11777777777");
            assertThat(response.getLastName()).isEqualTo("Name");
            assertThat(response.getEmail()).isEqualTo("old@brtickets.com");
            assertThat(response.getBirthday()).isEqualTo(LocalDate.of(1995, 6, 10));
            verify(pwdEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("updates every field when all of them are sent")
        void updatesAllFields() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
            when(pwdEncoder.encode("plain-password")).thenReturn("hashed-password");
            when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));

            UserResponseDTO response = userService.updateUser(user.getUserId(), request);

            assertThat(response.getFirstName()).isEqualTo("Yan");
            assertThat(response.getLastName()).isEqualTo("Carlos");
            assertThat(response.getEmail()).isEqualTo("yan@brtickets.com");
            assertThat(response.getPhone()).isEqualTo("11999999999");
            assertThat(response.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 15));
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        }

        @Test
        @DisplayName("re-encodes the password when a new one is sent")
        void encodesNewPassword() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
            when(pwdEncoder.encode("brand-new-password")).thenReturn("hashed-new-password");
            when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));

            UserRequestDTO partial = new UserRequestDTO();
            partial.setPassword("brand-new-password");

            userService.updateUser(user.getUserId(), partial);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-new-password");
        }

        @Test
        @DisplayName("refuses to update an inactive user and never saves")
        void refusesInactiveUser() {
            UserModel user = existingUser();
            user.setActive(false);
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateUser(user.getUserId(), request))
                    .isInstanceOf(UserIsNotActiveException.class)
                    .hasMessage("You cannot update an inactive user");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the user does not exist")
        void failsWhenUserDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(unknownId, request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found, we cannot update this user");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePwd")
    class UpdatePwd {

        @Test
        @DisplayName("saves the new password hashed")
        void updatesPassword() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
            when(pwdEncoder.encode("new-password")).thenReturn("hashed-new-password");

            userService.updatePwd(user.getUserId(), "new-password");

            verify(pwdEncoder).encode("new-password");
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-new-password");
        }

        @Test
        @DisplayName("fails when the user does not exist")
        void failsWhenUserDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updatePwd(unknownId, "new-password"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found, we cannot update your password");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("soft deletes the user by setting active to false")
        void deactivatesUser() {
            UserModel user = existingUser();
            when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

            userService.deactivateUser(user.getUserId());

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("fails when the user does not exist")
        void failsWhenUserDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found, we cannot deactivate this user");

            verify(userRepository, never()).save(any());
        }
    }
}
