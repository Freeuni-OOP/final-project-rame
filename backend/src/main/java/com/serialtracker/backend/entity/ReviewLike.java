package com.serialtracker.backend.entity;

import jakarta.persistence.*;

/**
 * ერთი მოწონება: likerUserId-მა დაალაიქა კონკრეტული რევიუ.
 * რევიუ ორ ცხრილში ცხოვრობს (episode / whole-show), ამიტომ ვინახავთ
 * reviewType + reviewId წყვილს (row id შესაბამის ცხრილში).
 */
@Entity
@Table(
        name = "review_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"liker_user_id", "review_type", "review_id"})
)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "liker_user_id", nullable = false)
    private Long likerUserId;

    @Column(name = "review_type", nullable = false)
    private String reviewType;   // "EPISODE" | "SHOW"

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    public ReviewLike() {}

    public ReviewLike(Long likerUserId, String reviewType, Long reviewId) {
        this.likerUserId = likerUserId;
        this.reviewType = reviewType;
        this.reviewId = reviewId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLikerUserId() { return likerUserId; }
    public void setLikerUserId(Long likerUserId) { this.likerUserId = likerUserId; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
}
