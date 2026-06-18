package com.serialtracker.backend.controller;

import com.serialtracker.backend.config.JwtUtils;
import com.serialtracker.backend.dto.LoginRequest;
import com.serialtracker.backend.dto.LoginResponse;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("LOGIN INPUT: " + loginRequest.getUsername());
        Optional<User> userOptional = userService.getUserByUsernameOrEmail(loginRequest.getUsername());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {

                String token = jwtUtils.generateJwtToken(user.getUsername());
                return ResponseEntity.ok(new LoginResponse(token));
            }
        }

        return ResponseEntity.badRequest().body("incorrect username or password!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody LoginRequest registerRequest) {
        try {
            User registeredUser = userService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok("User registered successfully with ID: " + registeredUser.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}