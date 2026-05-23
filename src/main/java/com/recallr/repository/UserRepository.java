package com.recallr.repository;

import com.recallr.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Map;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(@Size(min = 3, max = 10, message = "Username must be 3-10 characters") @NotBlank String username);

    Optional<User> findByUsername(String username);
}
