package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    long countByReviewTypeAndReviewId(String reviewType, Long reviewId);

    boolean existsByLikerUserIdAndReviewTypeAndReviewId(Long likerUserId, String reviewType, Long reviewId);

    Optional<ReviewLike> findByLikerUserIdAndReviewTypeAndReviewId(Long likerUserId, String reviewType, Long reviewId);

    // viewer-ის ყველა მოწონება — რომ ერთ მოთხოვნაში დავადგინოთ რომელი რევიუები აქვს ლაიქდადებული
    List<ReviewLike> findByLikerUserId(Long likerUserId);
}
