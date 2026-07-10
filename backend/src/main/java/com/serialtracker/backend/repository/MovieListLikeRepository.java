package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.MovieListLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieListLikeRepository extends JpaRepository<MovieListLike, Long> {

    // Used to check if this user already liked a list (for toggle logic).
    Optional<MovieListLike> findByLikerUserIdAndListId(Long likerUserId, Long listId);

    // All lists liked by a user, newest like first — backs the "Recently Liked" feed.
    List<MovieListLike> findByLikerUserIdOrderByLikedAtDesc(Long likerUserId);

    // Total like count for a list — shown on list cards.
    long countByListId(Long listId);
}
