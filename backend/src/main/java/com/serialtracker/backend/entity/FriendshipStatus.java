package com.serialtracker.backend.entity;

/**
 * Represents the state of a friendship/friend-request between two users.
 *
 * PENDING   - one user sent a request, the other hasn't responded yet.
 * ACCEPTED  - the recipient accepted; the two users are now friends.
 * DECLINED  - the recipient rejected the request.
 */
public enum FriendshipStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
