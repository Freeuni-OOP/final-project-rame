package com.serialtracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class LogControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserShowStatusRepository showStatusRepository;

    @Autowired
    private UserEpisodeStatusRepository episodeStatusRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String TEST_USERNAME = "log_test_user";
    private final int TEST_SHOW_ID = 555;

    private Long userId;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();

        User dummyUser = new User();
        dummyUser.setUsername(TEST_USERNAME);
        dummyUser.setPassword("password123");
        dummyUser.setEmail("logtest@gmail.com");
        userRepository.save(dummyUser);
        this.userId = dummyUser.getId();
    }

    // 1. ეპიზოდის ლოგი — 200 + ბაზაში ეპიზოდი ჩაჯდა რივიუ/რეიტინგით, შოუ WATCHING
    @Test
    public void logEpisode_savesEpisodeReview_andShowWatching() throws Exception {
        LogRequest req = new LogRequest();
        req.setUsername(TEST_USERNAME);
        req.setShowId(TEST_SHOW_ID);
        req.setSeasonNumber(2);
        req.setEpisodeNumber(5);
        req.setRating(4);
        req.setReview("great episode");
        req.setLiked(true);
        req.setWholeShow(false);

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Log saved"));

        // ეპიზოდის ცხრილში — რივიუ/რეიტინგი
        Optional<UserEpisodeStatus> ep = episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(userId, TEST_SHOW_ID, 2, 5);
        assertThat(ep).isPresent();
        assertThat(ep.get().getRating()).isEqualTo(4);
        assertThat(ep.get().getReview()).isEqualTo("great episode");
        assertThat(ep.get().isLiked()).isTrue();

        // შოუს ცხრილში — მხოლოდ WATCHING
        Optional<UserShowStatus> show = showStatusRepository.findByUserIdAndShowId(userId, TEST_SHOW_ID);
        assertThat(show).isPresent();
        assertThat(show.get().getStatus()).isEqualTo(SeriesStatus.WATCHING);
    }

    // 2. Whole Show — 200 + შოუ COMPLETED + რივიუ/რეიტინგი შოუზე, ეპიზოდი არ ჩაჯდა
    @Test
    public void logWholeShow_savesShowReview_completed() throws Exception {
        LogRequest req = new LogRequest();
        req.setUsername(TEST_USERNAME);
        req.setShowId(TEST_SHOW_ID);
        req.setWholeShow(true);
        req.setRating(5);
        req.setReview("whole show review");
        req.setLiked(true);

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // შოუს ცხრილში — COMPLETED + რივიუ/რეიტინგი
        Optional<UserShowStatus> show = showStatusRepository.findByUserIdAndShowId(userId, TEST_SHOW_ID);
        assertThat(show).isPresent();
        assertThat(show.get().getStatus()).isEqualTo(SeriesStatus.COMPLETED);
        assertThat(show.get().getRating()).isEqualTo(5);
        assertThat(show.get().getReview()).isEqualTo("whole show review");
        assertThat(show.get().isFavorite()).isTrue();
    }

    // 3. rewatch + watchDate — ეპიზოდზე სწორად ინახება
    @Test
    public void logEpisode_savesRewatchAndDate() throws Exception {
        LogRequest req = new LogRequest();
        req.setUsername(TEST_USERNAME);
        req.setShowId(TEST_SHOW_ID);
        req.setSeasonNumber(1);
        req.setEpisodeNumber(1);
        req.setRating(3);
        req.setRewatch(true);
        req.setWatchDate("2026-06-25");

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        Optional<UserEpisodeStatus> ep = episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(userId, TEST_SHOW_ID, 1, 1);
        assertThat(ep).isPresent();
        assertThat(ep.get().isRewatch()).isTrue();
        assertThat(ep.get().getWatchDate()).isEqualTo(LocalDate.of(2026, 6, 25));
    }

    // 4. იგივე ეპიზოდის ხელახალი ლოგი — update, დუბლიკატი არ იქმნება
    @Test
    public void logEpisode_twice_updatesInsteadOfDuplicate() throws Exception {
        LogRequest first = new LogRequest();
        first.setUsername(TEST_USERNAME);
        first.setShowId(TEST_SHOW_ID);
        first.setSeasonNumber(1);
        first.setEpisodeNumber(2);
        first.setRating(2);
        first.setReview("first");

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        LogRequest second = new LogRequest();
        second.setUsername(TEST_USERNAME);
        second.setShowId(TEST_SHOW_ID);
        second.setSeasonNumber(1);
        second.setEpisodeNumber(2);
        second.setRating(5);
        second.setReview("second");

        mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        // მხოლოდ ერთი ჩანაწერი, განახლებული მნიშვნელობებით
        assertThat(episodeStatusRepository.findByUserIdAndShowId(userId, TEST_SHOW_ID)).hasSize(1);
        Optional<UserEpisodeStatus> ep = episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(userId, TEST_SHOW_ID, 1, 2);
        assertThat(ep).isPresent();
        assertThat(ep.get().getRating()).isEqualTo(5);
        assertThat(ep.get().getReview()).isEqualTo("second");
    }

    // 5. არარსებული იუზერი — saveLog exception-ს აგდებს (controller-ს handler არ აქვს)
    @Test
    public void log_unknownUser_throws() {
        LogRequest req = new LogRequest();
        req.setUsername("ghost_user_does_not_exist");
        req.setShowId(TEST_SHOW_ID);
        req.setWholeShow(true);
        req.setRating(3);

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
        ).hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}