package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.UserEpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEpisodeStatusRepository extends JpaRepository<UserEpisodeStatus, Long> {
    Optional<UserEpisodeStatus> findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(
            Long userId, int showId, int seasonNumber, int episodeNumber);

    List<UserEpisodeStatus> findByUserIdAndShowId(Long userId, int showId);

    boolean existsByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(Long userId, int showId, Integer seasonNumber, Integer episodeNumber);
    void deleteByUserIdAndShowId(Long userId, int showId);
    List<UserEpisodeStatus> findByUserId(Long userId);

    List<UserEpisodeStatus> findByShowIdAndReviewIsNotNull(int showId);

    // Diary: ამ იუზერის ყველა დათარიღებული ეპიზოდ-ჩანაწერი
    List<UserEpisodeStatus> findByUserIdAndWatchDateIsNotNull(Long userId);

    // Feed: მოცემული იუზერების ეპიზოდ-ჩანაწერები, სადაც რივიუ ან რეიტინგია
    @Query("SELECT e FROM UserEpisodeStatus e WHERE e.userId IN :userIds " +
            "AND (e.review IS NOT NULL OR e.rating IS NOT NULL)")
    List<UserEpisodeStatus> findFeedEntriesByUserIds(@Param("userIds") Collection<Long> userIds);

    // Public feed: ყველა იუზერის ეპიზოდ-ჩანაწერი, სადაც რივიუ ან რეიტინგია
    @Query("SELECT e FROM UserEpisodeStatus e WHERE e.review IS NOT NULL OR e.rating IS NOT NULL")
    List<UserEpisodeStatus> findAllReviewed();
}