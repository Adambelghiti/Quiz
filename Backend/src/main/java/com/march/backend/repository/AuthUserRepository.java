package com.march.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.march.backend.model.AuthUser;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    AuthUser findByUsername(String username);
    boolean existsByUsername(String username);

    /**
     * Case-insensitive variants used for login and registration so that a user
     * cannot register a second account that differs from an existing one only
     * by letter case.
     */
    AuthUser findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}