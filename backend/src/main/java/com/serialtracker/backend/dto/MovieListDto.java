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
 * likeCount and likedByMe are injected by the controller using
 * MovieListLikeRepository — kept out of the service layer to avoid
 * adding a second repository dependency there.
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
    private final long likeCount;
    private final boolean likedByMe;
    private final List<MovieListItemDto> items;

    public MovieListDto(Long id, String ownerUsername, String name, String description,
                         boolean isPublic, LocalDateTime createdAt,
                         long likeCount, boolean likedByMe,
                         List<MovieListItemDto> items) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
        this.items = items;
    }

    // Convenience factory used when likeCount/likedByMe aren't needed
    // (e.g. internal calls that don't pass a viewing user).
    public static MovieListDto from(MovieList list, String ownerUsername) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(),
                list.getDescription(), list.isPublic(), list.getCreatedAt(),
                0L, false, null);
    }

    // Factory with like info but no items (list-of-lists views).
    public static MovieListDto from(MovieList list, String ownerUsername,
                                     long likeCount, boolean likedByMe) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(),
                list.getDescription(), list.isPublic(), list.getCreatedAt(),
                likeCount, likedByMe, null);
    }

    // Factory with like info and items (full detail view).
    public static MovieListDto from(MovieList list, String ownerUsername,
                                     long likeCount, boolean likedByMe,
                                     List<MovieListItemDto> items) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(),
                list.getDescription(), list.isPublic(), list.getCreatedAt(),
                likeCount, likedByMe, items);
    }

    // Legacy factory kept for callers that haven't been updated yet.
    public static MovieListDto from(MovieList list, String ownerUsername, List<MovieListItemDto> items) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(),
                list.getDescription(), list.isPublic(), list.getCreatedAt(),
                0L, false, items);
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    @JsonProperty("isPublic")
    public boolean isPublic() { return isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getLikeCount() { return likeCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public List<MovieListItemDto> getItems() { return items; }
}
