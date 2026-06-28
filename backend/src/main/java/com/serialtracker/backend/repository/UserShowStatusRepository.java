package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserShowStatusRepository extends JpaRepository<UserShowStatus, Long> {
    Optional<UserShowStatus> findByUserIdAndShowId(Long userId, int showId);

    List<UserShowStatus> findByUserIdAndStatus(Long userId, SeriesStatus status);

    List<UserShowStatus> findByUserIdAndStatusOrderByIdDesc(Long userId, SeriesStatus status);
}