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
    private UserService userService; // შემოგვაქვს სერვისი (ბიზნეს ლოგიკა)

    @Autowired
    private PasswordEncoder passwordEncoder; // შემოგვაქვს პაროლის შემდარებელი

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {

        Optional<User> userOptional = userService.getUserByUsername(loginRequest.getUsername());

        // 2. თუ იუზერი არსებობს, ვამოწმებთ პაროლს
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // matches მეთოდი ადარებს მოსულ ღია პაროლს და ბაზიდან წამოღებულ ჰეშს
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {

                // 3. თუ დაემთხვა, ვუგენერირებთ ტოკენს
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
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok("User registered successfully with ID: " + registeredUser.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}