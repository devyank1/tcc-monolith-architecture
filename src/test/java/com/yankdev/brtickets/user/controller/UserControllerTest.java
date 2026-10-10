package com.yankdev.brtickets.user.controller;

import tools.jackson.databind.ObjectMapper;
import com.yankdev.brtickets.shared.exception.EmailAlreadyExistsException;
import com.yankdev.brtickets.shared.exception.InvalidCredentialsException;
import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.shared.security.SecurityConfig;
import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticatedUserProvider userProvider;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID userId;
    private UserRequestDTO request;
    private UserResponseDTO response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        request = new UserRequestDTO();
        request.setFirstName("Yan");
        request.setLastName("Carlos");
        request.setEmail("yan@brtickets.com");
        request.setPassword("plain-password");
        request.setCpf("12345678901");
        request.setPhone("11999999999");
        request.setBirthday(LocalDate.of(2000, 1, 15));

        response = new UserResponseDTO();
        response.setUserId(userId);
        response.setFirstName("Yan");
        response.setLastName("Carlos");
        response.setEmail("yan@brtickets.com");
        response.setRole(UserRole.USER);
        response.setActive(true);
        response.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /users returns 201 with the created user")
    @WithAnonymousUser
    void registerReturnsCreated() throws Exception {
        when(userService.register(any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("yan@brtickets.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /users returns 409 when the email already exists")
    @WithAnonymousUser
    void registerReturnsConflict() throws Exception {
        when(userService.register(any(UserRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists."));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /users/login returns 200 with the token")
    @WithAnonymousUser
    void loginReturnsOk() throws Exception {
        response.setToken("jwt-token");
        when(userService.login(any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /users/login returns 401 for invalid credentials")
    @WithAnonymousUser
    void loginReturnsUnauthorized() throws Exception {
        when(userService.login(any(UserRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("Invalid credentials."));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users returns 200 with the list for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void findAllUsersReturnsOkForAdmin() throws Exception {
        when(userService.findAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("yan@brtickets.com"));
    }

    @Test
    @DisplayName("GET /users returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void findAllUsersReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).findAllUsers();
    }

    @Test
    @DisplayName("GET /users/{userId} returns the authenticated user")
    @WithMockUser
    void findUserReturnsOk() throws Exception {
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.findUser(userId)).thenReturn(response);

        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("GET /users/{userId} returns 404 when the user does not exist")
    @WithMockUser
    void findUserReturnsNotFound() throws Exception {
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.findUser(userId)).thenThrow(new UserNotFoundException("User not found by USER ID."));

        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /users/{userId} returns 200 with the updated user")
    @WithMockUser
    void updateUserReturnsOk() throws Exception {
        response.setFirstName("NewName");
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.updateUser(eq(userId), any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewName"));
    }

    @Test
    @DisplayName("PUT /users/{userId}/newPassword returns 204")
    @WithMockUser
    void updatePasswordReturnsNoContent() throws Exception {
        when(userProvider.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(put("/users/{userId}/newPassword", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updatePwd(userId, "plain-password");
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 204 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteUserReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deactivateUser(userId);
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void deleteUserReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isForbidden());

        verify(userService, never()).deactivateUser(any());
    }
}
