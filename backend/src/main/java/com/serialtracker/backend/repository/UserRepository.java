package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring will automatically: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    //check if it exists
    boolean existsByUsername(String username);
}