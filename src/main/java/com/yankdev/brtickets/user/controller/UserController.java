package com.yankdev.brtickets.user.controller;

import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping
    @Tag(name = "User Registration", description = "Register a new user")
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRequestDTO request) {

        UserResponseDTO user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @Tag(name = "User Login", description = "Authenticate a user and return user details")
    public ResponseEntity<UserResponseDTO> login(@RequestBody UserRequestDTO request) {

    UserResponseDTO user = userService.login(request);
    return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "Get All Users", description = "Retrieve a list of all registered users (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserResponseDTO>> findAllUsers() {

        List<UserResponseDTO> user = userService.findAllUsers();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    @Tag(name = "Get User", description = "Retrieve details of a specific user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDTO> findUser(@PathVariable UUID userId) {

        UserResponseDTO user = userService.findUser(userId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{userId}")
    @Tag(name = "Update User", description = "Update details of a specific user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID userId, @RequestBody UserRequestDTO request) {

        UserResponseDTO user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/newPassword")
    @Tag(name = "Update Password", description = "Update the password of a specific user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID userId, @RequestBody UserRequestDTO request) {

        userService.updatePwd(userId, request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Tag(name = "Delete User", description = "Deactivate a specific user")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {

        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
