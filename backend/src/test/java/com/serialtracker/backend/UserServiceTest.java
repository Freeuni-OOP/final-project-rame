package com.serialtracker.backend;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_Success() {
        String username = "newuser";
        String email = "new@freeuni.edu.ge";
        String rawPassword = "password123";
        String encodedPassword = "hashed_password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        User savedUser = new User(username, email, encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(username, email, rawPassword);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_ThrowsException_WhenEmailExists() {
        // Given
        String username = "anotheruser";
        String email = "mkekn23@freeuni.edu.ge";  // alrready there
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(username, email, password);
        });

        assertEquals("Email is already registered!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void testGetUserByUsernameOrEmail_Success() {
        // Given
        String loginInput = "mkekn23@freeuni.edu.ge";
        User mockUser = new User("testuser", loginInput, "hashed_password");

        when(userRepository.findByUsernameOrEmail(loginInput, loginInput))
                .thenReturn(Optional.of(mockUser));

        // When
        Optional<User> result = userService.getUserByUsernameOrEmail(loginInput);

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(loginInput, result.get().getEmail());
    }
}