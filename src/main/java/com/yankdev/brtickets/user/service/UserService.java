package com.yankdev.brtickets.user.service;

import com.yankdev.brtickets.user.dto.UserRequestDTO;
import com.yankdev.brtickets.user.dto.UserResponseDTO;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder pwdEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder pwdEncoder) {
        this.userRepository = userRepository;
        this.pwdEncoder = pwdEncoder;
    }

    public UserResponseDTO register(UserRequestDTO userRequest) {

        if (!userRepository.existsByEmail(userRequest.getEmail()) || !userRepository.existsByCpf(userRequest.getCpf())) {
            UserModel newUser = new UserModel();
            newUser.setFirstName(userRequest.getFirstName());
            newUser.setLastName(userRequest.getLastName());
            newUser.setEmail(userRequest.getEmail());
            newUser.setPasswordHash(pwdEncoder.encode(userRequest.getPassword()));
            newUser.setCpf(userRequest.getCpf());
            newUser.setPhone(userRequest.getPhone());
            newUser.setBirthday(userRequest.getBirthday());
            newUser.setActive(true);
            newUser.setRole(UserRole.FANS);

            UserModel savedUser = userRepository.save(newUser);

            return UserResponseDTO.from(savedUser);

        } else {
            throw new IllegalArgumentException("Email ja cadastrado por favor, recupere a senha ou tente de novo.");
        }
    }
}
