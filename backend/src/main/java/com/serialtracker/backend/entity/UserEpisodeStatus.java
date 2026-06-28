package com.serialtracker.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_episodes_status")
public class UserEpisodeStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private int showId;
    private int seasonNumber;
    private int episodeNumber;
    @Column(nullable = true)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    private boolean liked;
    private boolean rewatch;
    private java.time.LocalDate watchDate;

    public UserEpisodeStatus() {}
    public UserEpisodeStatus(Long userId, int showId, int seasonNumber, int episodeNumber) {
        this.userId = userId;
        this.showId = showId;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.rating = null; // 🟢 ახალი სტატუსის შექმნისას რეიტინგი თავიდან ცარიელია
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
    public int getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }
    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { this.episodeNumber = episodeNumber; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public java.time.LocalDate getWatchDate() { return watchDate; }
    public void setWatchDate(java.time.LocalDate watchDate) { this.watchDate = watchDate; }
}