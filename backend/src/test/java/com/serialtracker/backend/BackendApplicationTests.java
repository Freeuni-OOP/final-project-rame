package com.serialtracker.backend;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateUser() {
        User user = new User();
        user.setUsername("test_user");
        user.setPassword("password123");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("test_user", savedUser.getUsername());
    }
}