package com.serialtracker.backend;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.FavoriteShowRepository;
import com.serialtracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class FavoriteShowControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FavoriteShowRepository favoriteRepository;

    private final String USERNAME = "fav_test_user";

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword("password123");
        user.setEmail("fav_test@gmail.com");
        userRepository.save(user);
    }

    // 1. GET /api/favorites?username=X — ახალ იუზერს ცარიელი სია აქვს
    @Test
    void getFavorites_returnsEmptyList_forNewUser() throws Exception {
        mockMvc.perform(get("/api/favorites").param("username", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // 2. POST — შოუს დამატება მუშაობს და პასუხში ის ჩანს
    @Test
    void addFavorite_success_showAppearsInList() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "1399"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains(1399)));
    }

    // 3. POST — დუბლიკატი → 400
    @Test
    void addFavorite_duplicate_returns400() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "1399"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "1399"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("already in your favorites")));
    }

    // 4. POST — 5-ის ლიმიტი — მეექვსე შოუ → 400
    @Test
    void addFavorite_exceedsLimit_returns400() throws Exception {
        int[] shows = {1, 2, 3, 4, 5};
        for (int showId : shows) {
            mockMvc.perform(post("/api/favorites")
                            .param("username", USERNAME)
                            .param("showId", String.valueOf(showId)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("only have 5")));
    }

    // 5. DELETE — ამოშლა მუშაობს, სია მცირდება
    @Test
    void removeFavorite_success_listUpdates() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "100"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // 6. DELETE — არარსებული შოუს ამოშლა ჩუმად გადის (არ ისვრის შეცდომას)
    @Test
    void removeFavorite_notPresent_silentlySucceeds() throws Exception {
        mockMvc.perform(delete("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // 7. 5-ის ლიმიტის შემდეგ ამოშლა + ახლის დამატება → წარმატებული
    @Test
    void addFavorite_afterRemovingOne_succeedsWhenAtLimit() throws Exception {
        int[] shows = {1, 2, 3, 4, 5};
        for (int showId : shows) {
            mockMvc.perform(post("/api/favorites")
                            .param("username", USERNAME)
                            .param("showId", String.valueOf(showId)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(delete("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "3"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/favorites")
                        .param("username", USERNAME)
                        .param("showId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    // 8. არარსებული იუზერი → RuntimeException
    @Test
    void getFavorites_unknownUser_throws() {
        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/favorites").param("username", "ghost_user_xyz"))
        ).hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}