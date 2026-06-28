package com.serialtracker.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A user-created list of shows (e.g. "Best Sci-Fi of 2024").
 *
 * This entity only describes the list itself (name, description, owner,
 * visibility). The shows that belong to it live in MovieListItem rows,
 * the same way a Friendship row is separate from a User row.
 */
@Entity
@Table(name = "movie_lists")
public class MovieList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We store just the owner's id (like UserShowStatus.userId), not a
    // full User reference, to avoid pulling in a lazy-loaded User object
    // every time we just want to know whose list this is.
    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MovieList() {}

    public MovieList(Long ownerId, String name, String description, boolean isPublic) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
