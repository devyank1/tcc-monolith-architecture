package com.yankdev.brtickets.user.service;

import com.yankdev.brtickets.shared.exception.*;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder pwdEncoder;
    private final JwtUtils jwtUtils;

    public UserService(UserRepository userRepository,
                       PasswordEncoder pwdEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.pwdEncoder = pwdEncoder;
        this.jwtUtils = jwtUtils;
    }

    public UserResponseDTO register(UserRequestDTO userRequest) {

        if (userRepository.existsByEmail(userRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        if (userRepository.existsByCpf(userRequest.getCpf())) {
            throw new CpfAlreadyExistsException("CPF already exists.");
        }

        UserModel newUser = new UserModel();
        newUser.setFirstName(userRequest.getFirstName());
        newUser.setLastName(userRequest.getLastName());
        newUser.setEmail(userRequest.getEmail());
        newUser.setPasswordHash(pwdEncoder.encode(userRequest.getPassword()));
        newUser.setCpf(userRequest.getCpf());
        newUser.setPhone(userRequest.getPhone());
        newUser.setBirthday(userRequest.getBirthday());
        newUser.setActive(true);
        newUser.setRole(UserRole.USER);
        newUser.setCreatedAt(LocalDateTime.now());

        UserModel savedUser = userRepository.save(newUser);

        return UserResponseDTO.from(savedUser);

    }

    public UserResponseDTO login(UserRequestDTO request) {

        UserModel user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials."));

        if (!pwdEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials.");
        }

        String token = jwtUtils.generateToken(user);
        UserResponseDTO response = UserResponseDTO.from(user);
        response.setToken(token);

        return response;
    }

    public UserResponseDTO findUser(UUID userId) {

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found by USER ID."));

        return UserResponseDTO.from(user);
    }

    public List<UserResponseDTO> findAllUsers() {

        List<UserModel> user = userRepository.findAll();
        return user.stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    public UserResponseDTO updateUser(UUID userId, UserRequestDTO request) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, we cannot update this user"));

        if (!user.isActive()) {
            throw new UserIsNotActiveException("You cannot update an inactive user");
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBirthday() != null) user.setBirthday(request.getBirthday());
        if (request.getPassword() != null) user.setPasswordHash(pwdEncoder.encode(request.getPassword()));

        UserModel updatedUser = userRepository.save(user);

        return UserResponseDTO.from(updatedUser);
    }

    public void updatePwd(UUID userId, String password) {

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, we cannot update your password"));

        user.setPasswordHash(pwdEncoder.encode(password));
        userRepository.save(user);
    }

    public void deactivateUser(UUID userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, we cannot deactivate this user"));
        user.setActive(false);
        userRepository.save(user);
    }
}
