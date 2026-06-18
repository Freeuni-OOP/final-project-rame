package com.serialtracker.backend;

import com.serialtracker.backend.controller.AuthController;
import com.serialtracker.backend.dto.LoginRequest;
import com.serialtracker.backend.dto.LoginResponse;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.config.JwtUtils;
import com.serialtracker.backend.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationFlowTest {

    private UserServiceImpl userService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private AuthController authController;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = mock(JwtUtils.class);

        userService = new UserServiceImpl(userRepository, passwordEncoder);

        authController = new AuthController();
        ReflectionTestUtils.setField(authController, "userService", userService);
        ReflectionTestUtils.setField(authController, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authController, "jwtUtils", jwtUtils);
    }


    @Test
    void testRegisterUser_Success() {
        String username = "giorgi123";
        String email = "giorgi@freeuni.edu.ge";
        String password = "myPassword";
        String encodedPassword = "hashed_myPassword";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        User mockSavedUser = new User(username, email, encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        User result = userService.registerUser(username, email, password);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_ThrowsException_WhenUsernameExists() {
        String username = "existingUser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(username, "any@mail.com", "pass");
        });
    }

    @Test
    void testRegisterUser_ThrowsException_WhenEmailExists() {
        String email = "mkekn23@freeuni.edu.ge";
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            userService.registerUser("freshUser", email, "pass");
        });
    }

    @Test
    void testGetUserByUsernameOrEmail_Success() {
        String loginInput = "mkekn23@freeuni.edu.ge";
        User mockUser = new User("giorgi", loginInput, "pass");
        when(userRepository.findByUsernameOrEmail(loginInput, loginInput)).thenReturn(Optional.of(mockUser));

        Optional<User> result = userService.getUserByUsernameOrEmail(loginInput);

        assertTrue(result.isPresent());
        assertEquals("giorgi", result.get().getUsername());
    }



    @Test
    void testAuthController_Login_Success() {
        LoginRequest loginRequest = new LoginRequest("giorgi", "", "pass123");
        User mockUser = new User("giorgi", "giorgi@mail.com", "hashed_pass");

        when(userRepository.findByUsernameOrEmail("giorgi", "giorgi")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("pass123", "hashed_pass")).thenReturn(true);
        when(jwtUtils.generateJwtToken("giorgi")).thenReturn("mocked-jwt-token");

        ResponseEntity<?> response = authController.loginUser(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof LoginResponse);
        assertEquals("mocked-jwt-token", ((LoginResponse) response.getBody()).getToken());
    }

    @Test
    void testAuthController_Login_Failure() {
        LoginRequest loginRequest = new LoginRequest("wrongUser", "", "pass");
        when(userRepository.findByUsernameOrEmail("wrongUser", "wrongUser")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.loginUser(loginRequest);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("incorrect username or password!", response.getBody());
    }

    @Test
    void testAuthController_Register_Success() {
        LoginRequest regRequest = new LoginRequest("newUser", "new@mail.com", "pass");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);

        User savedUser = new User("newUser", "new@mail.com", "hashed");
        savedUser.setId(99L); // ვაყენებთ ID-ს ტესტისთვის
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ResponseEntity<?> response = authController.registerUser(regRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User registered successfully with ID: 99", response.getBody());
    }

    @Test
    void testAuthController_Register_Failure_Exception() {
        LoginRequest regRequest = new LoginRequest("badUser", "bad@mail.com", "pass");
        when(userRepository.existsByUsername("badUser")).thenReturn(true); // გამოიწვევს RuntimeException-ს სერვისში

        ResponseEntity<?> response = authController.registerUser(regRequest);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Username is already taken!", response.getBody());
    }


    @Test
    void testModelsAndResponsesCoverage() {
        // User Entity (ID, Getters, Setters)
        User user = new User();
        user.setId(1L);
        user.setUsername("giorgi");
        user.setEmail("giorgi@mail.com");
        user.setPassword("pass");

        assertEquals(1L, user.getId());
        assertEquals("giorgi", user.getUsername());
        assertEquals("giorgi@mail.com", user.getEmail());
        assertEquals("pass", user.getPassword());

        // LoginRequest 3-პარამეტრიანი კონსტრუქტორი და სეთერები
        LoginRequest requestWithConstructor = new LoginRequest("testUser", "test@mail.com", "123");
        assertEquals("testUser", requestWithConstructor.getUsername());
        assertEquals("test@mail.com", requestWithConstructor.getEmail());
        assertEquals("123", requestWithConstructor.getPassword());

        LoginRequest requestWithSetters = new LoginRequest();
        requestWithSetters.setUsername("ani");
        requestWithSetters.setEmail("ani@mail.com");
        requestWithSetters.setPassword("pass");

        assertEquals("ani", requestWithSetters.getUsername());
        assertEquals("ani@mail.com", requestWithSetters.getEmail());
        assertEquals("pass", requestWithSetters.getPassword());

        // LoginResponse (კონსტრუქტორი, სეთერი, გეთერი)
        LoginResponse response = new LoginResponse("initial-token");
        response.setToken("final-jwt-token");
        assertEquals("final-jwt-token", response.getToken());
    }
}