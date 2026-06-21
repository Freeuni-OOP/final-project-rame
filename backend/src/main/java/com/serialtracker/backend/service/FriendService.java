package com.serialtracker.backend.service;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.dto.FriendSuggestionDto;

import java.util.List;

public interface FriendService {

    Friendship sendRequest(String requesterUsername, String recipientUsername);

    Friendship acceptRequest(Long friendshipId, String acceptingUsername);

    Friendship declineRequest(Long friendshipId, String decliningUsername);

    void removeFriend(String username, String otherUsername);

    List<User> getFriends(String username);

    List<Friendship> getPendingIncomingRequests(String username);

    List<Friendship> getSentRequests(String username);

    List<FriendSuggestionDto> getSuggestedFriends(String username);
}
