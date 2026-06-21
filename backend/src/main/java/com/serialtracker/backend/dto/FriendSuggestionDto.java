package com.serialtracker.backend.dto;

/**
 * A "people you may know" suggestion: someone who isn't your friend yet
 * (and has no pending request with you either way), ranked by how many
 * friends you already have in common with them.
 */
public class FriendSuggestionDto {

    private final String username;
    private final int mutualFriendCount;

    public FriendSuggestionDto(String username, int mutualFriendCount) {
        this.username = username;
        this.mutualFriendCount = mutualFriendCount;
    }

    public String getUsername() { return username; }
    public int getMutualFriendCount() { return mutualFriendCount; }
}
