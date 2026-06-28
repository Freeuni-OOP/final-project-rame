package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.UserShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserShowStatusRepository extends JpaRepository<UserShowStatus, Long> {
    Optional<UserShowStatus> findByUserIdAndShowId(Long userId, int showId);
}