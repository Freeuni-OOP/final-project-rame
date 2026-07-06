package com.serialtracker.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private int showId;
    private String showName;
    private String actionType; // მაგ. "WATCHED_EPISODE", "COMPLETED", "STATUS_UPDATE", "LIKED"
    private String detail;      // დამატებითი ინფორმაცია (მაგ. "Marked as Plan to Watch" ან "Rated 4/5")
    private LocalDateTime createdAt;
    private String posterPath;
    private Double rating; // null თუ არ დაურეითებია


    public Activity() {}

    public Activity(String username, int showId, String showName, String actionType, String detail) {
        this.username = username;
        this.showId = showId;
        this.showName = showName;
        this.actionType = actionType;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public String getShowName() { return showName; }
    public void setShowName(String showName) { this.showName = showName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Getter და Setter-ები:
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}