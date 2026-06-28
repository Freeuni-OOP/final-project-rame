package com.serialtracker.backend;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ShowsTrackingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private final String TEST_USERNAME = "test_user";
    private final int TEST_SHOW_ID = 999;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();

        User dummyUser = new User();
        dummyUser.setUsername(TEST_USERNAME);
        dummyUser.setPassword("password123");
        dummyUser.setEmail("testuser@gmail.com");
        userRepository.save(dummyUser);
    }

    @Test
    public void testUpdateShowStatus() throws Exception {
        mockMvc.perform(post("/api/tracking/show-status")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID))
                        .param("status", SeriesStatus.COMPLETED.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Show status updated to:")));
    }

    @Test
    public void testToggleEpisode() throws Exception {
        // პირველი მოთხოვნა: მონიშვნა ნანახად
        mockMvc.perform(post("/api/tracking/toggle-episode")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID))
                        .param("seasonNumber", "1")
                        .param("episodeNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Episode marked as WATCHED"));

        // მეორე მოთხოვნა: მოხსნა ნანახიდან
        mockMvc.perform(post("/api/tracking/toggle-episode")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID))
                        .param("seasonNumber", "1")
                        .param("episodeNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Episode marked as UNWATCHED"));
    }

    @Test
    public void testToggleFavorite() throws Exception {
        mockMvc.perform(post("/api/tracking/toggle-favorite")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testGetWatchedEpisodes() throws Exception {
        mockMvc.perform(get("/api/tracking/watched-episodes")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID)))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetShowStatus() throws Exception {
        // ჯერ შევქმნათ სტატუსი, რომ ბაზაში იდოს
        mockMvc.perform(post("/api/tracking/show-status")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID))
                        .param("status", SeriesStatus.COMPLETED.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // ახლა შევამოწმოთ get-status მეთოდი
        mockMvc.perform(get("/api/tracking/get-status")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID)))
                .andExpect(status().isOk());
    }

    @Test
    public void testToggleEpisodeFirstTime() throws Exception {
        mockMvc.perform(post("/api/tracking/toggle-episode")
                        .param("username", TEST_USERNAME)
                        .param("showId", String.valueOf(TEST_SHOW_ID))
                        .param("seasonNumber", "2")
                        .param("episodeNumber", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Episode marked as WATCHED"));
    }
}

