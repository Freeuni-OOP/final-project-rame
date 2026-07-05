package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.UserEpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserEpisodeStatusRepository extends JpaRepository<UserEpisodeStatus, Long> {
    Optional<UserEpisodeStatus> findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(
            Long userId, int showId, int seasonNumber, int episodeNumber);

    List<UserEpisodeStatus> findByUserIdAndShowId(Long userId, int showId);

    boolean existsByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(Long userId, int showId, Integer seasonNumber, Integer episodeNumber);
    void deleteByUserIdAndShowId(Long userId, int showId);

    List<UserEpisodeStatus> findByShowIdAndReviewIsNotNull(int showId);

    // Diary: ამ იუზერის ყველა დათარიღებული ეპიზოდ-ჩანაწერი
    List<UserEpisodeStatus> findByUserIdAndWatchDateIsNotNull(Long userId);
}