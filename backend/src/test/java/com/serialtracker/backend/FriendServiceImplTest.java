package com.serialtracker.backend;

import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.entity.FriendshipStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.FriendshipRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.service.FriendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendServiceImplTest {

    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private FriendServiceImpl friendService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        friendshipRepository = mock(FriendshipRepository.class);
        userRepository = mock(UserRepository.class);
        friendService = new FriendServiceImpl(friendshipRepository, userRepository);

        alice = new User("alice", "alice@mail.com", "hashedpass");
        alice.setId(1L);
        bob = new User("bob", "bob@mail.com", "hashedpass");
        bob.setId(2L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @Test
    void sendRequest_success() {
        when(friendshipRepository.findBetweenUsers(alice, bob)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Friendship result = friendService.sendRequest("alice", "bob");

        assertEquals(FriendshipStatus.PENDING, result.getStatus());
        assertEquals("alice", result.getRequester().getUsername());
        assertEquals("bob", result.getRecipient().getUsername());
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @Test
    void sendRequest_throwsException_whenSendingToSelf() {
        assertThrows(RuntimeException.class, () -> friendService.sendRequest("alice", "alice"));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendRequest_throwsException_whenFriendshipAlreadyExists() {
        Friendship existing = new Friendship(alice, bob, FriendshipStatus.PENDING);
        when(friendshipRepository.findBetweenUsers(alice, bob)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> friendService.sendRequest("alice", "bob"));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void acceptRequest_success() {
        Friendship pending = new Friendship(alice, bob, FriendshipStatus.PENDING);
        pending.setId(10L);
        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Friendship result = friendService.acceptRequest(10L, "bob");

        assertEquals(FriendshipStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void acceptRequest_throwsException_whenAccepterIsNotRecipient() {
        Friendship pending = new Friendship(alice, bob, FriendshipStatus.PENDING);
        pending.setId(10L);
        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(pending));

        // alice is the requester, not the recipient - she should not be able to accept her own request
        assertThrows(RuntimeException.class, () -> friendService.acceptRequest(10L, "alice"));
    }

    @Test
    void declineRequest_success() {
        Friendship pending = new Friendship(alice, bob, FriendshipStatus.PENDING);
        pending.setId(11L);
        when(friendshipRepository.findById(11L)).thenReturn(Optional.of(pending));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Friendship result = friendService.declineRequest(11L, "bob");

        assertEquals(FriendshipStatus.DECLINED, result.getStatus());
    }

    @Test
    void removeFriend_success() {
        Friendship accepted = new Friendship(alice, bob, FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findBetweenUsers(alice, bob)).thenReturn(Optional.of(accepted));

        friendService.removeFriend("alice", "bob");

        verify(friendshipRepository, times(1)).delete(accepted);
    }

    @Test
    void removeFriend_throwsException_whenNotFriends() {
        when(friendshipRepository.findBetweenUsers(alice, bob)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> friendService.removeFriend("alice", "bob"));
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void getFriends_returnsTheOtherUserForEachAcceptedFriendship() {
        Friendship accepted = new Friendship(alice, bob, FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findAcceptedFriendshipsOf(alice)).thenReturn(List.of(accepted));

        List<User> friends = friendService.getFriends("alice");

        assertEquals(1, friends.size());
        assertEquals("bob", friends.get(0).getUsername());
    }

    @Test
    void getPendingIncomingRequests_returnsOnlyPendingRequestsToThisUser() {
        Friendship pending = new Friendship(alice, bob, FriendshipStatus.PENDING);
        when(friendshipRepository.findByRecipientAndStatus(bob, FriendshipStatus.PENDING))
                .thenReturn(List.of(pending));

        List<Friendship> result = friendService.getPendingIncomingRequests("bob");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getRequester().getUsername());
    }
}
