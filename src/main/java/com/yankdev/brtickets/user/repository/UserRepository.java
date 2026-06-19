package com.yankdev.brtickets.user.repository;

import com.yankdev.brtickets.user.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserModel, UUID> {
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    Optional<UserModel> findByEmail(String email);
}
