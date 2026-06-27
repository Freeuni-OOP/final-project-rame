package com.serialtracker.backend;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserShowStatusRepositoryTest {

    @Autowired
    private UserShowStatusRepository repository;

    private final Long USER_ID = 1L;
    private final int SHOW_ID = 555;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // 1. findByUserIdAndShowId — სწორ შოუს პოულობს ყველა ველით
    @Test
    void findByUserIdAndShowId_returnsCorrectShow() {
        UserShowStatus show = new UserShowStatus(USER_ID, SHOW_ID, SeriesStatus.COMPLETED);
        show.setRating(5);
        show.setReview("masterpiece");
        show.setFavorite(true);
        show.setRewatch(true);
        show.setWatchDate(LocalDate.of(2026, 6, 25));
        repository.save(show);

        Optional<UserShowStatus> found = repository.findByUserIdAndShowId(USER_ID, SHOW_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(SeriesStatus.COMPLETED);
        assertThat(found.get().getRating()).isEqualTo(5);
        assertThat(found.get().getReview()).isEqualTo("masterpiece");
        assertThat(found.get().isFavorite()).isTrue();
        assertThat(found.get().isRewatch()).isTrue();
        assertThat(found.get().getWatchDate()).isEqualTo(LocalDate.of(2026, 6, 25));
    }

    // 2. findByUserIdAndShowId — არასწორ შოუზე ცარიელს აბრუნებს
    @Test
    void findByUserIdAndShowId_wrongShow_returnsEmpty() {
        repository.save(new UserShowStatus(USER_ID, SHOW_ID, SeriesStatus.WATCHING));

        Optional<UserShowStatus> found = repository.findByUserIdAndShowId(USER_ID, 999);

        assertThat(found).isEmpty();
    }

    // 3. findByUserIdAndShowId — იუზერებს არ ურევს
    @Test
    void findByUserIdAndShowId_isolatesByUser() {
        repository.save(new UserShowStatus(USER_ID, SHOW_ID, SeriesStatus.WATCHING));
        repository.save(new UserShowStatus(2L, SHOW_ID, SeriesStatus.COMPLETED)); // სხვა იუზერი, იგივე შოუ

        Optional<UserShowStatus> found = repository.findByUserIdAndShowId(USER_ID, SHOW_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(USER_ID);
        assertThat(found.get().getStatus()).isEqualTo(SeriesStatus.WATCHING);
    }

    // 4. update — არსებული ჩანაწერი ნახლდება, ახალი არ იქმნება
    @Test
    void save_updatesExistingShow() {
        UserShowStatus show = new UserShowStatus(USER_ID, SHOW_ID, SeriesStatus.WATCHING);
        show.setRating(2);
        UserShowStatus saved = repository.save(show);

        // იგივე ჩანაწერი ვცვლით
        saved.setRating(5);
        saved.setStatus(SeriesStatus.COMPLETED);
        repository.save(saved);

        assertThat(repository.findAll()).hasSize(1); // ერთი ჩანაწერი, არა ორი
        Optional<UserShowStatus> found = repository.findByUserIdAndShowId(USER_ID, SHOW_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(5);
        assertThat(found.get().getStatus()).isEqualTo(SeriesStatus.COMPLETED);
    }
}