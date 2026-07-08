package com.serialtracker.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class MovieListDto {

    private final Long id;
    private final String ownerUsername;
    private final String name;
    private final String description;
    private final boolean isPublic;
    private final LocalDateTime createdAt;
    private final List<MovieListItemDto> items;
    private String ownerProfilePicture; // 🟢 ინახავს ლისტის შემქმნელის ფოტოს Base64-ში

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

    // 🟢 განახლებული მეთოდი იუზერის ობიექტით
    public static MovieListDto from(MovieList list, User owner) {
        MovieListDto dto = new MovieListDto(list.getId(), owner.getUsername(), list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), null);

        if (owner.getProfilePicture() != null) {
            dto.setOwnerProfilePicture(java.util.Base64.getEncoder().encodeToString(owner.getProfilePicture()));
        }
        return dto;
    }

    public static MovieListDto from(MovieList list, String ownerUsername, List<MovieListItemDto> items) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), items);
    }

    // 🟢 განახლებული მეთოდი დეტალური ხედვისთვის (items-ით)
    public static MovieListDto from(MovieList list, User owner, List<MovieListItemDto> items) {
        MovieListDto dto = new MovieListDto(list.getId(), owner.getUsername(), list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), items);

        if (owner.getProfilePicture() != null) {
            dto.setOwnerProfilePicture(java.util.Base64.getEncoder().encodeToString(owner.getProfilePicture()));
        }
        return dto;
    }

    // 🔵 ძველი მეთოდი (Fallback-ისთვის, თუ სადმე იუზერის გარეშე იძახებენ)
    public static MovieListDto from(MovieList list, String ownerUsername) {
        return new MovieListDto(list.getId(), ownerUsername, list.getName(), list.getDescription(),
                list.isPublic(), list.getCreatedAt(), null);
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    @JsonProperty("isPublic")
    public boolean isPublic() { return isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MovieListItemDto> getItems() { return items; }
    public String getOwnerProfilePicture() { return ownerProfilePicture; }

    // 🟢 სეთერი ფრონტენდისთვის
    public void setOwnerProfilePicture(String ownerProfilePicture) {
        this.ownerProfilePicture = ownerProfilePicture;
    }
}