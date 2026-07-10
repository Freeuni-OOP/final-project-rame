package com.serialtracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.ReviewLikeRepository;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class ReviewControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private UserEpisodeStatusRepository episodeStatusRepository;
    @Autowired private UserShowStatusRepository showStatusRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String USERNAME = "review_test_user";
    private final int SHOW_ID = 1234;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword("password123");
        user.setEmail("review_test@gmail.com");
        userRepository.save(user);

        // ეპიზოდის ლოგი — review-თი
        LogRequest ep = new LogRequest();
        ep.setUsername(USERNAME);
        ep.setShowId(SHOW_ID);
        ep.setSeasonNumber(1);
        ep.setEpisodeNumber(3);
        ep.setRating(4);
        ep.setReview("great episode");
        ep.setWholeShow(false);

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ep)))
                .andExpect(status().isOk());

        // Whole Show ლოგი — review-თი
        LogRequest show = new LogRequest();
        show.setUsername(USERNAME);
        show.setShowId(SHOW_ID);
        show.setWholeShow(true);
        show.setRating(5);
        show.setReview("amazing show overall");

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(show)))
                .andExpect(status().isOk());
    }

    // 1. GET /api/reviews/{showId} — ყველა review ჩანს (ფილტრი გამორთული)
    @Test
    void getReviews_noFilter_returnsBoth() throws Exception {
        mockMvc.perform(get("/api/reviews/" + SHOW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // 2. GET — სპოილერ-ფილტრი: S1E3 ჩანს S1E3-ის ჩათვლით
    @Test
    void getReviews_spoilerFilter_includesCurrentEpisode() throws Exception {
        mockMvc.perform(get("/api/reviews/" + SHOW_ID)
                        .param("season", "1")
                        .param("episode", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].review", hasItem("great episode")));
    }

    // 3. GET — სპოილერ-ფილტრი: S1E2 არ ჩანს S1E2-ის მომხმარებელს, რადგან review S1E3-ზეა
    @Test
    void getReviews_spoilerFilter_hidesLaterEpisode() throws Exception {
        mockMvc.perform(get("/api/reviews/" + SHOW_ID)
                        .param("season", "1")
                        .param("episode", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].review", not(hasItem("great episode"))));
    }

    // 4. POST /api/reviews/like — toggle on: liked=true, likeCount=1
    @Test
    void toggleLike_firstTime_returnsLikedTrue() throws Exception {
        // ვიღებთ episode-ის row id-ს
        var eps = episodeStatusRepository.findByUserIdAndShowId(
                userRepository.findByUsername(USERNAME).get().getId(), SHOW_ID);
        Long epId = eps.get(0).getId();

        mockMvc.perform(post("/api/reviews/like")
                        .param("username", USERNAME)
                        .param("reviewType", "EPISODE")
                        .param("reviewId", String.valueOf(epId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    // 5. POST like twice — toggle off: liked=false, likeCount=0
    @Test
    void toggleLike_twice_returnsLikedFalse() throws Exception {
        var eps = episodeStatusRepository.findByUserIdAndShowId(
                userRepository.findByUsername(USERNAME).get().getId(), SHOW_ID);
        Long epId = eps.get(0).getId();

        mockMvc.perform(post("/api/reviews/like")
                        .param("username", USERNAME)
                        .param("reviewType", "EPISODE")
                        .param("reviewId", String.valueOf(epId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reviews/like")
                        .param("username", USERNAME)
                        .param("reviewType", "EPISODE")
                        .param("reviewId", String.valueOf(epId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    // 6. GET — likedByMe სწორად ჩანს review სიაში
    @Test
    void getReviews_likedByMe_reflectsToggle() throws Exception {
        var eps = episodeStatusRepository.findByUserIdAndShowId(
                userRepository.findByUsername(USERNAME).get().getId(), SHOW_ID);
        Long epId = eps.get(0).getId();

        mockMvc.perform(post("/api/reviews/like")
                        .param("username", USERNAME)
                        .param("reviewType", "EPISODE")
                        .param("reviewId", String.valueOf(epId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/" + SHOW_ID)
                        .param("username", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.review=='great episode')].likedByMe", contains(true)));
    }
}