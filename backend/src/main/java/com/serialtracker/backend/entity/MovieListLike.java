package com.serialtracker.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One "like" of a MovieList by a user.
 * likedAt lets us return the user's recently-liked lists in chronological order.
 */
@Entity
@Table(
        name = "movie_list_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"liker_user_id", "list_id"})
)
public class MovieListLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "liker_user_id", nullable = false)
    private Long likerUserId;

    @Column(name = "list_id", nullable = false)
    private Long listId;

    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt = LocalDateTime.now();

    public MovieListLike() {}

    public MovieListLike(Long likerUserId, Long listId) {
        this.likerUserId = likerUserId;
        this.listId = listId;
        this.likedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLikerUserId() { return likerUserId; }
    public void setLikerUserId(Long likerUserId) { this.likerUserId = likerUserId; }

    public Long getListId() { return listId; }
    public void setListId(Long listId) { this.listId = listId; }

    public LocalDateTime getLikedAt() { return likedAt; }
    public void setLikedAt(LocalDateTime likedAt) { this.likedAt = likedAt; }
}
