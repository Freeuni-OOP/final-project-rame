package com.serialtracker.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One show inside one list. A MovieList with 10 shows in it has 10 of
 * these rows, each pointing back at the list and at a TMDB show id —
 * the same idea as one UserEpisodeStatus row per watched episode.
 *
 * showId is an int (not a foreign key) because, like everywhere else in
 * this app, shows are not stored locally — they come from TMDB and are
 * only ever referenced by their TMDB id.
 */
@Entity
@Table(name = "movie_list_items")
public class MovieListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long listId;

    @Column(nullable = false)
    private int showId;

    // Lets us preserve the order the user arranged shows in, instead of
    // relying on insertion order or id order.
    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    public MovieListItem() {}

    public MovieListItem(Long listId, int showId, int position) {
        this.listId = listId;
        this.showId = showId;
        this.position = position;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getListId() { return listId; }
    public void setListId(Long listId) { this.listId = listId; }
    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
