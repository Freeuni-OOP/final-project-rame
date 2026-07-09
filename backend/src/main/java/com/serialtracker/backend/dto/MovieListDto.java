package com.serialtracker.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serialtracker.backend.entity.MovieList;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What we send back for a list itself. ownerUsername is filled in by the
 * caller (the service only stores ownerId) since resolving it requires
 * a UserRepository lookup the DTO shouldn't have to know about.
 *
 * items is optional: null when we're just returning "here are your lists"
 * (e.g. a list-of-lists view), populated when returning one list's full detail.
 */
public class MovieListDto {

    private final Long id;
    private final String ownerUsername;
    private final String name;
    private final String description;
    private final boolean isPublic;
    private final LocalDateTime createdAt;
    private final List<MovieListItemDto> items;

    public MovieListDto(Long id, String ownerUsername, String name, String description,
                         boolean isPublic, LocalDateTime createdAt, List<MovieListItemDto> items) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.createdAt = createdAt;
        this.items = items;
    }

    public static MovieListDto from(MovieList list, String ownerUsername) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), null);
    }

    public static MovieListDto from(MovieList list, String ownerUsername, List<MovieListItemDto> items) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), items);
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    @JsonProperty("isPublic")
    public boolean isPublic() { return isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MovieListItemDto> getItems() { return items; }
}
