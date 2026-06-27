package com.serialtracker.backend;

import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserEpisodeStatusRepositoryTest {

    @Autowired
    private UserEpisodeStatusRepository repository;

    private final Long USER_ID = 1L;
    private final int SHOW_ID = 555;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByUserIdAndShowIdAndSeasonAndEpisode_returnsCorrectEpisode() {
        UserEpisodeStatus ep = new UserEpisodeStatus(USER_ID, SHOW_ID, 2, 5);
        ep.setRating(4);
        ep.setReview("nice");
        ep.setLiked(true);
        ep.setRewatch(true);
        ep.setWatchDate(LocalDate.of(2026, 6, 25));
        repository.save(ep);

        Optional<UserEpisodeStatus> found = repository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(USER_ID, SHOW_ID, 2, 5);

        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(4);
        assertThat(found.get().getReview()).isEqualTo("nice");
        assertThat(found.get().isLiked()).isTrue();
        assertThat(found.get().isRewatch()).isTrue();
        assertThat(found.get().getWatchDate()).isEqualTo(LocalDate.of(2026, 6, 25));
    }

    @Test
    void findByUserIdAndShowIdAndSeasonAndEpisode_wrongEpisode_returnsEmpty() {
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 2, 5));

        Optional<UserEpisodeStatus> found = repository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(USER_ID, SHOW_ID, 2, 6);

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndShowId_returnsAllEpisodesForShow() {
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 1, 1));
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 1, 2));
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 2, 1));
        repository.save(new UserEpisodeStatus(USER_ID, 999, 1, 1)); // სხვა შოუ

        List<UserEpisodeStatus> episodes = repository.findByUserIdAndShowId(USER_ID, SHOW_ID);

        assertThat(episodes).hasSize(3);
    }

    @Test
    void existsByUserIdAndShowIdAndSeasonAndEpisode_works() {
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 3, 7));

        boolean exists = repository
                .existsByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(USER_ID, SHOW_ID, 3, 7);
        boolean notExists = repository
                .existsByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(USER_ID, SHOW_ID, 3, 8);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void findBy_isolatesByUser() {
        repository.save(new UserEpisodeStatus(USER_ID, SHOW_ID, 1, 1));
        repository.save(new UserEpisodeStatus(2L, SHOW_ID, 1, 1)); // სხვა იუზერი

        Optional<UserEpisodeStatus> found = repository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(USER_ID, SHOW_ID, 1, 1);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(USER_ID);
    }
}