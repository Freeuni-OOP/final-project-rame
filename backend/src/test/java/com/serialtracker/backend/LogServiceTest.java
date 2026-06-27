package com.serialtracker.backend;

import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import com.serialtracker.backend.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserShowStatusRepository showStatusRepository;
    @Mock
    private UserEpisodeStatusRepository episodeStatusRepository;

    @InjectMocks
    private LogService logService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Niakoo", "niakoo@test.com", "hashedpw");
        testUser.setId(1L);
    }

    private LogRequest baseRequest() {
        LogRequest req = new LogRequest();
        req.setUsername("Niakoo");
        req.setShowId(124364);
        req.setRating(4);
        req.setReview("good");
        req.setLiked(true);
        return req;
    }

    // 1. User not found -> exception, neither repo is touched
    @Test
    void saveLog_userNotFound_throws() {
        LogRequest req = baseRequest();
        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logService.saveLog(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(showStatusRepository, never()).save(any());
        verify(episodeStatusRepository, never()).save(any());
    }

    // 2. Episode log -> review/rating saved on EPISODE, show only gets a save (WATCHING)
    @Test
    void saveLog_episode_savesReviewOnEpisode_showOnlyWatching() {
        LogRequest req = baseRequest();
        req.setSeasonNumber(2);
        req.setEpisodeNumber(5);
        req.setWholeShow(false);

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.empty());
        when(episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(1L, 124364, 2, 5))
                .thenReturn(Optional.empty());

        logService.saveLog(req);

        // show was saved (status = WATCHING)
        verify(showStatusRepository).save(any(UserShowStatus.class));

        // episode holds the review/rating/like
        ArgumentCaptor<UserEpisodeStatus> epCaptor = ArgumentCaptor.forClass(UserEpisodeStatus.class);
        verify(episodeStatusRepository).save(epCaptor.capture());
        UserEpisodeStatus savedEp = epCaptor.getValue();
        assertThat(savedEp.getSeasonNumber()).isEqualTo(2);
        assertThat(savedEp.getEpisodeNumber()).isEqualTo(5);
        assertThat(savedEp.getRating()).isEqualTo(4);
        assertThat(savedEp.getReview()).isEqualTo("good");
        assertThat(savedEp.isLiked()).isTrue();
    }

    // 3. Episode update -> existing row is updated, no new row created
    @Test
    void saveLog_episode_updatesExisting() {
        LogRequest req = baseRequest();
        req.setSeasonNumber(1);
        req.setEpisodeNumber(3);
        req.setRating(5);
        req.setReview("updated");

        UserEpisodeStatus existing = new UserEpisodeStatus(1L, 124364, 1, 3);
        existing.setRating(2);
        existing.setReview("old");

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.empty());
        when(episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(1L, 124364, 1, 3))
                .thenReturn(Optional.of(existing));

        logService.saveLog(req);

        ArgumentCaptor<UserEpisodeStatus> epCaptor = ArgumentCaptor.forClass(UserEpisodeStatus.class);
        verify(episodeStatusRepository).save(epCaptor.capture());
        UserEpisodeStatus saved = epCaptor.getValue();
        assertThat(saved).isSameAs(existing); // same object (no insert)
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getReview()).isEqualTo("updated");
    }

    // 4. Whole Show -> show gets COMPLETED + review/rating, episode repo untouched
    @Test
    void saveLog_wholeShow_savesReviewOnShow_completed_noEpisode() {
        LogRequest req = baseRequest();
        req.setWholeShow(true);
        req.setSeasonNumber(null);
        req.setEpisodeNumber(null);

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.empty());

        logService.saveLog(req);

        ArgumentCaptor<UserShowStatus> showCaptor = ArgumentCaptor.forClass(UserShowStatus.class);
        verify(showStatusRepository).save(showCaptor.capture());
        UserShowStatus saved = showCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SeriesStatus.COMPLETED);
        assertThat(saved.getRating()).isEqualTo(4);
        assertThat(saved.getReview()).isEqualTo("good");
        assertThat(saved.isFavorite()).isTrue();

        verify(episodeStatusRepository, never()).save(any());
    }

    // 5. Episode -> rewatch + watchDate saved on episode
    @Test
    void saveLog_episode_savesRewatchAndDate() {
        LogRequest req = baseRequest();
        req.setSeasonNumber(1);
        req.setEpisodeNumber(1);
        req.setRewatch(true);
        req.setWatchDate("2026-06-25");

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.empty());
        when(episodeStatusRepository
                .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(1L, 124364, 1, 1))
                .thenReturn(Optional.empty());

        logService.saveLog(req);

        ArgumentCaptor<UserEpisodeStatus> epCaptor = ArgumentCaptor.forClass(UserEpisodeStatus.class);
        verify(episodeStatusRepository).save(epCaptor.capture());
        UserEpisodeStatus saved = epCaptor.getValue();
        assertThat(saved.isRewatch()).isTrue();
        assertThat(saved.getWatchDate()).isEqualTo(LocalDate.of(2026, 6, 25));
    }

    // 6. Whole Show -> rewatch + watchDate saved on show
    @Test
    void saveLog_wholeShow_savesRewatchAndDate() {
        LogRequest req = baseRequest();
        req.setWholeShow(true);
        req.setRewatch(true);
        req.setWatchDate("2026-01-15");

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.empty());

        logService.saveLog(req);

        ArgumentCaptor<UserShowStatus> showCaptor = ArgumentCaptor.forClass(UserShowStatus.class);
        verify(showStatusRepository).save(showCaptor.capture());
        UserShowStatus saved = showCaptor.getValue();
        assertThat(saved.isRewatch()).isTrue();
        assertThat(saved.getWatchDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    // 7. Zero rating -> existing value is NOT overwritten
    @Test
    void saveLog_zeroRating_keepsExisting() {
        LogRequest req = baseRequest();
        req.setWholeShow(true);
        req.setRating(0);
        req.setReview(null);

        UserShowStatus existing = new UserShowStatus(1L, 124364, SeriesStatus.WATCHING);
        existing.setRating(3);
        existing.setReview("kept");

        when(userRepository.findByUsername("Niakoo")).thenReturn(Optional.of(testUser));
        when(showStatusRepository.findByUserIdAndShowId(1L, 124364)).thenReturn(Optional.of(existing));

        logService.saveLog(req);

        ArgumentCaptor<UserShowStatus> showCaptor = ArgumentCaptor.forClass(UserShowStatus.class);
        verify(showStatusRepository).save(showCaptor.capture());
        UserShowStatus saved = showCaptor.getValue();
        assertThat(saved.getRating()).isEqualTo(3);
        assertThat(saved.getReview()).isEqualTo("kept");
    }
}