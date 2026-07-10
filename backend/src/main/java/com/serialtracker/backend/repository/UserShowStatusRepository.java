package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserShowStatusRepository extends JpaRepository<UserShowStatus, Long> {
    Optional<UserShowStatus> findByUserIdAndShowId(Long userId, int showId);

    List<UserShowStatus> findByUserIdAndStatus(Long userId, SeriesStatus status);

    List<UserShowStatus> findByUserIdAndStatusOrderByIdDesc(Long userId, SeriesStatus status);

    List<UserShowStatus> findByUserId(Long userId);

    // ამ შოუს ყველა "Whole Show" რივიუ (მხოლოდ ის, სადაც review დაწერილია)
    List<UserShowStatus> findByShowIdAndReviewIsNotNull(int showId);

    // Diary: ამ იუზერის ყველა დათარიღებული whole-show ჩანაწერი
    List<UserShowStatus> findByUserIdAndWatchDateIsNotNull(Long userId);

    // Likes: ამ იუზერის მოწონებული (heart / isFavorite=true) შოუები, ბოლო-პირველი
    @org.springframework.data.jpa.repository.Query(
            "SELECT s FROM UserShowStatus s WHERE s.userId = :userId AND s.isFavorite = true ORDER BY s.id DESC")
    List<UserShowStatus> findLikedByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    // Films count: ბოლომდე ნანახი (COMPLETED) შოუების რაოდენობა
    long countByUserIdAndStatus(Long userId, SeriesStatus status);

    // Feed: მოცემული იუზერების whole-show ჩანაწერები, სადაც რივიუ ან რეიტინგია
    @org.springframework.data.jpa.repository.Query(
            "SELECT s FROM UserShowStatus s WHERE s.userId IN :userIds " +
                    "AND (s.review IS NOT NULL OR s.rating IS NOT NULL)")
    List<UserShowStatus> findFeedEntriesByUserIds(
            @org.springframework.data.repository.query.Param("userIds") java.util.Collection<Long> userIds);

    // Public feed: ყველა იუზერის whole-show ჩანაწერი, სადაც რივიუ ან რეიტინგია
    @org.springframework.data.jpa.repository.Query(
            "SELECT s FROM UserShowStatus s WHERE s.review IS NOT NULL OR s.rating IS NOT NULL")
    List<UserShowStatus> findAllReviewed();
}