package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Looks for a row where "user" sent the request to "other" (one specific direction).
    Optional<Friendship> findByRequesterAndRecipient(User requester, User recipient);

    // Looks for any row connecting the two users, no matter who sent the request.
    // We need this to stop someone from sending a second request to a person
    // who already sent THEM one, or who is already their friend.
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :a AND f.recipient = :b) OR " +
            "(f.requester = :b AND f.recipient = :a)")
    Optional<Friendship> findBetweenUsers(@Param("a") User a, @Param("b") User b);

    // All requests sent TO "user" that are still waiting for a response.
    List<Friendship> findByRecipientAndStatus(User recipient, FriendshipStatus status);

    // All requests "user" has SENT that are still waiting for a response.
    // Mirror of findByRecipientAndStatus above, just from the other side.
    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);

    // All accepted friendships where "user" appears on either side.
    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' " +
            "AND (f.requester = :user OR f.recipient = :user)")
    List<Friendship> findAcceptedFriendshipsOf(@Param("user") User user);
}
