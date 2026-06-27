package com.serialtracker.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_show_statuses")
public class UserShowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private int showId;

    private boolean rewatch;

    private LocalDate watchDate;

    @Enumerated(EnumType.STRING)
    private SeriesStatus status;

    @Column(nullable = true)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    private boolean isFavorite = false;

    // კონსტრუქტორები
    public UserShowStatus() {}

    public UserShowStatus(Long userId, int showId, SeriesStatus status) {
        this.userId = userId;
        this.showId = showId;
        this.status = status;
        this.rating = null; // 🟢 ახალი სტატუსის შექმნისას რეიტინგი თავიდან ცარიელია
    }

    // გეთერები და სეთერები
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public LocalDate getWatchDate() { return watchDate; }
    public void setWatchDate(LocalDate watchDate) { this.watchDate = watchDate; }

    public SeriesStatus getStatus() { return status; }
    public void setStatus(SeriesStatus status) { this.status = status; }


    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}