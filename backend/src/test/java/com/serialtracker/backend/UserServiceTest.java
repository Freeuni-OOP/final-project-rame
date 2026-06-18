package com.serialtracker.backend;

import com.serialtracker.backend.entity.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void testUserValidation() {
        User user = new User();
        user.setUsername("tornike");
        assertEquals("tornike", user.getUsername());
    }
}