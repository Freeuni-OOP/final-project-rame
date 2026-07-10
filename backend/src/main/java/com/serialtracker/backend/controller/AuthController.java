package com.serialtracker.backend.controller;

import com.serialtracker.backend.config.JwtUtils;
import com.serialtracker.backend.dto.LoginRequest;
import com.serialtracker.backend.dto.LoginResponse;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.service.UserService;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.ActivityRepository;
import com.serialtracker.backend.repository.RecommendationRepository;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

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

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/profile", consumes = "multipart/form-data")
    public ResponseEntity<?> updateProfile(
            @RequestParam("currentUsername") String currentUsername,
            @RequestParam(value = "newUsername", required = false) String newUsername,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "profilePicture", required = false) org.springframework.web.multipart.MultipartFile file) {

        try {
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

            boolean usernameChanged = false;

            // 1. თუ ფრონტენდმა გამოატანა ახალი სახელი
            if (newUsername != null && !newUsername.trim().isEmpty() && !newUsername.equals(currentUsername)) {
                if (userRepository.findByUsername(newUsername).isPresent()) {
                    return ResponseEntity.badRequest().body("Username '" + newUsername + "' is already taken!");
                }
                user.setUsername(newUsername);
                usernameChanged = true;
            }

            // 2. თუ ფრონტენდმა გამოატანა ახალი პაროლი
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }

            // 3. თუ ფრონტენდმა გამოატანა ფოტო
            if (file != null && !file.isEmpty()) {
                user.setProfilePicture(file.getBytes());
            }

            // ვინახავთ მონაცემებს
            userRepository.saveAndFlush(user);

            // 4. თუ სახელი მართლა შეიცვალა, მხოლოდ მაშინ ვასწორებთ სხვა ცხრილებს
            if (usernameChanged) {
                activityRepository.updateUsernameInActivity(currentUsername, newUsername);
                recommendationRepository.updateTargetUsername(currentUsername, newUsername);
                recommendationRepository.updateSenderUsername(currentUsername, newUsername);
            }

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Profile updated successfully!",
                    "usernameChanged", usernameChanged,
                    "username", user.getUsername()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}