package com.serialtracker.backend.service;

import com.serialtracker.backend.entity.User;
import java.util.Optional;

public interface UserService {
    Optional<User> getUserByUsername(String username);

    User registerUser(String username, String email, String rawPassword);
    Optional<User> getUserByUsernameOrEmail(String loginInput);
}