package com.serialtracker.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderUsername;

    @Column(nullable = false)
    private String targetUsername;

    @Column(nullable = false)
    private int showId;

    @Column(nullable = false)
    private String showName;

    @Column(length = 500)
    private String comment;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Recommendation() {}

    public Recommendation(String senderUsername, String targetUsername, int showId, String showName, String comment) {
        this.senderUsername = senderUsername;
        this.targetUsername = targetUsername;
        this.showId = showId;
        this.showName = showName;
        this.comment = comment;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
    public String getShowName() { return showName; }
    public void setShowName(String showName) { this.showName = showName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}