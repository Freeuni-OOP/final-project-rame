package com.serialtracker.backend.dto;

import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.entity.FriendshipStatus;

import java.time.LocalDateTime;

/**
 * What we actually send back to the frontend for a Friendship.
 *
 * We never return the Friendship entity directly: it holds full User
 * objects (requester/recipient), which are lazily-loaded JPA proxies.
 * Trying to serialize those straight to JSON either throws a
 * LazyInitializationException (if the database session already closed)
 * or leaks the user's password hash and other fields we don't want exposed.
 * Mapping to this small, flat DTO avoids both problems.
 */
public class FriendshipDto {

    private final Long id;
    private final String requesterUsername;
    private final String recipientUsername;
    private final FriendshipStatus status;
    private final LocalDateTime createdAt;

    public FriendshipDto(Long id, String requesterUsername, String recipientUsername,
                          FriendshipStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.requesterUsername = requesterUsername;
        this.recipientUsername = recipientUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static FriendshipDto from(Friendship friendship) {
        return new FriendshipDto(
                friendship.getId(),
                friendship.getRequester().getUsername(),
                friendship.getRecipient().getUsername(),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getRequesterUsername() { return requesterUsername; }
    public String getRecipientUsername() { return recipientUsername; }
    public FriendshipStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
