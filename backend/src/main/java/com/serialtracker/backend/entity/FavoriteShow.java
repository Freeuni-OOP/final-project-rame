package com.serialtracker.backend.entity;

import jakarta.persistence.*;

/**
 * იუზერის "Favorite TV Shows" — მაქსიმუმ 5 ხელით არჩეული სერიალი (Letterboxd-ის
 * "favorites"-ის მსგავსად). ცალკე UserShowStatus.isFavorite-სგან: ეს კურირებული
 * top-5-ია, არა ყველა მოწონებული.
 */
@Entity
@Table(
        name = "favorite_shows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "show_id"})
)
public class FavoriteShow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private int showId;

    public FavoriteShow() {}

    public FavoriteShow(Long userId, int showId) {
        this.userId = userId;
        this.showId = showId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
}
