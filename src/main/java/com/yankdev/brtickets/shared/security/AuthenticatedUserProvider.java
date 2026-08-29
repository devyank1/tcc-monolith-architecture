package com.yankdev.brtickets.shared.security;

import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public AuthenticatedUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserModel getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
