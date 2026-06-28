package com.serialtracker.backend.dto;

import com.serialtracker.backend.entity.MovieListItem;

import java.time.LocalDateTime;

/**
 * What we send back for one show inside a list.
 */
public class MovieListItemDto {

    private final Long id;
    private final int showId;
    private final int position;
    private final LocalDateTime addedAt;

    public MovieListItemDto(Long id, int showId, int position, LocalDateTime addedAt) {
        this.id = id;
        this.showId = showId;
        this.position = position;
        this.addedAt = addedAt;
    }

    public static MovieListItemDto from(MovieListItem item) {
        return new MovieListItemDto(item.getId(), item.getShowId(), item.getPosition(), item.getAddedAt());
    }

    public Long getId() { return id; }
    public int getShowId() { return showId; }
    public int getPosition() { return position; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
