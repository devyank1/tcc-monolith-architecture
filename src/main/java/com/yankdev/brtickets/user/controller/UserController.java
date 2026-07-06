package com.yankdev.brtickets.user.controller;

import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRequestDTO request) {

        UserResponseDTO user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@RequestBody UserRequestDTO request) {

    UserResponseDTO user = userService.login(request);
    return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAllUsers() {

        List<UserResponseDTO> user = userService.findAllUsers();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> findUser(@PathVariable UUID userId) {

        UserResponseDTO user = userService.findUser(userId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID userId, @RequestBody UserRequestDTO request) {

        UserResponseDTO user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/newPassword")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID userId, @RequestBody UserRequestDTO request) {

        userService.updatePwd(userId, request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {

        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
