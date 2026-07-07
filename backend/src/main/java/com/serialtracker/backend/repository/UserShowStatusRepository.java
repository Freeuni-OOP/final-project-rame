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
}