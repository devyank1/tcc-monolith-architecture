package com.yankdev.brtickets.user.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserModel persistedUser;

    @BeforeEach
    void setUp() {
        persistedUser = entityManager.persistAndFlush(user("yan@brtickets.com", "12345678901"));
    }

    private UserModel user(String email, String cpf) {
        UserModel user = new UserModel();
        user.setFirstName("Yan");
        user.setLastName("Carlos");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setCpf(cpf);
        user.setPhone("11999999999");
        user.setBirthday(LocalDate.of(2000, 1, 15));
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("generates a UUID on insert")
    void generatesUuidOnInsert() {
        assertThat(persistedUser.getUserId()).isNotNull();
    }

    @Test
    @DisplayName("existsByEmail finds the stored email")
    void existsByEmailReturnsTrue() {
        assertThat(userRepository.existsByEmail("yan@brtickets.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail is false for an unknown email")
    void existsByEmailReturnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@brtickets.com")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail is case sensitive")
    void existsByEmailIsCaseSensitive() {
        assertThat(userRepository.existsByEmail("YAN@BRTICKETS.COM")).isFalse();
    }

    @Test
    @DisplayName("existsByCpf finds the stored CPF")
    void existsByCpfReturnsTrue() {
        assertThat(userRepository.existsByCpf("12345678901")).isTrue();
    }

    @Test
    @DisplayName("existsByCpf is false for an unknown CPF")
    void existsByCpfReturnsFalse() {
        assertThat(userRepository.existsByCpf("00000000000")).isFalse();
    }

    @Test
    @DisplayName("findByEmail returns the whole user")
    void findByEmailReturnsUser() {
        var found = userRepository.findByEmail("yan@brtickets.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(persistedUser.getUserId());
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(found.get().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("findByEmail returns empty for an unknown email")
    void findByEmailReturnsEmpty() {
        assertThat(userRepository.findByEmail("ghost@brtickets.com")).isEmpty();
    }

    @Test
    @DisplayName("the database rejects a duplicated email")
    void rejectsDuplicatedEmail() {
        UserModel duplicate = user("yan@brtickets.com", "99999999999");

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("the database rejects a duplicated CPF")
    void rejectsDuplicatedCpf() {
        UserModel duplicate = user("other@brtickets.com", "12345678901");

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("findById returns empty for an id that was never stored")
    void findByIdReturnsEmpty() {
        assertThat(userRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
