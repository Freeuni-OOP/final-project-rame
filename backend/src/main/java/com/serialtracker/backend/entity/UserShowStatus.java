package com.serialtracker.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_show_statuses")
public class UserShowStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private int showId;

    @Enumerated(EnumType.STRING)
    private SeriesStatus status;

    private boolean isFavorite = false;

    public UserShowStatus() {}
    public UserShowStatus(Long userId, int showId, SeriesStatus status) {
        this.userId = userId;
        this.showId = showId;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
    public SeriesStatus getStatus() { return status; }
    public void setStatus(SeriesStatus status) { this.status = status; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}