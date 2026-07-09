package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.FavoriteShow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteShowRepository extends JpaRepository<FavoriteShow, Long> {

    List<FavoriteShow> findByUserIdOrderByIdAsc(Long userId);

    Optional<FavoriteShow> findByUserIdAndShowId(Long userId, int showId);

    long countByUserId(Long userId);
}
