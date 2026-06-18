package com.serialtracker.backend;

import com.serialtracker.backend.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    @Test
    void testLoginRequestGettersAndSetters() {
        LoginRequest request = new LoginRequest();
        request.setUsername("mkekn23@freeuni.edu.ge"); // ჩვენი ჰიბრიდული ინფუთი
        request.setPassword("myPassword123");

        assertEquals("mkekn23@freeuni.edu.ge", request.getUsername());
        assertEquals("myPassword123", request.getPassword());
    }

    @Test
    void testLoginRequestConstructor() {
        LoginRequest request = new LoginRequest("testuser", "test@freeuni.edu.ge", "pass123");

        assertEquals("testuser", request.getUsername());
        assertEquals("test@freeuni.edu.ge", request.getEmail());
        assertEquals("pass123", request.getPassword());
    }
}