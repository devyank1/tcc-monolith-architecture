package com.yankdev.brtickets.user.model;

import com.yankdev.brtickets.user.model.enums.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(unique = true)
    private String cpf;
    private String phone;
    private LocalDate birthday;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getUserId() {return userId;}

    public void setUserId(UUID userId) {this.userId = userId;}

    public String getFirstName() {return firstName;}

    public void setFirstName(String firstName) {this.firstName = firstName;}

    public String getLastName() {return lastName;}

    public void setLastName(String lastName) {this.lastName = lastName;}

    public String getEmail() {return email;}

    public void setEmail(String email) {this.email = email;}

    public String getPasswordHash() {return passwordHash;}

    public void setPasswordHash(String passwordHash) {this.passwordHash = passwordHash;}

    public String getCpf() {return cpf;}

    public void setCpf(String cpf) {this.cpf = cpf;}

    public String getPhone() {return phone;}

    public void setPhone(String phone) {this.phone = phone;}

    public LocalDate getBirthday() {return birthday;}

    public void setBirthday(LocalDate birthday) {this.birthday = birthday;}

    public UserRole getRole() {return role;}

    public void setRole(UserRole role) {this.role = role;}

    public boolean isActive() {return active;}

    public void setActive(boolean active) {this.active = active;}

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public LocalDateTime getUpdatedAt() {return updatedAt;}

    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;}
}
