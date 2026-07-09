package com.serialtracker.backend.service;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.repository.FriendshipRepository;
import com.serialtracker.backend.entity.FriendshipStatus;
import com.serialtracker.backend.dto.FriendSuggestionDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendServiceImpl(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Friendship sendRequest(String requesterUsername, String recipientUsername) {
        if (requesterUsername.equals(recipientUsername)) {
            throw new RuntimeException("You cannot send a friend request to yourself!");
        }

        User requester = getUserOrThrow(requesterUsername);
        User recipient = getUserOrThrow(recipientUsername);

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(requester, recipient);
        if (existing.isPresent()) {
            throw new RuntimeException("A friendship or pending request already exists between these users.");
        }

        Friendship friendship = new Friendship(requester, recipient, FriendshipStatus.PENDING);
        return friendshipRepository.save(friendship);
    }

    @Override
    public Friendship acceptRequest(Long friendshipId, String acceptingUsername) {
        Friendship friendship = getFriendshipOrThrow(friendshipId);

        if (!friendship.getRecipient().getUsername().equals(acceptingUsername)) {
            throw new RuntimeException("Only the recipient of the request can accept it.");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending.");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }

    @Override
    public Friendship declineRequest(Long friendshipId, String decliningUsername) {
        Friendship friendship = getFriendshipOrThrow(friendshipId);

        if (!friendship.getRecipient().getUsername().equals(decliningUsername)) {
            throw new RuntimeException("Only the recipient of the request can decline it.");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending.");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        return friendshipRepository.save(friendship);
    }

    @Override
    public void removeFriend(String username, String otherUsername) {
        User user = getUserOrThrow(username);
        User other = getUserOrThrow(otherUsername);

        Friendship friendship = friendshipRepository.findBetweenUsers(user, other)
                .orElseThrow(() -> new RuntimeException("These users are not friends."));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new RuntimeException("These users are not friends.");
        }

        friendshipRepository.delete(friendship);
    }

    @Override
    public List<User> getFriends(String username) {
        User user = getUserOrThrow(username);

        return friendshipRepository.findAcceptedFriendshipsOf(user).stream()
                .map(f -> f.getRequester().getUsername().equals(username) ? f.getRecipient() : f.getRequester())
                .toList();
    }

    @Override
    public List<Friendship> getPendingIncomingRequests(String username) {
        User user = getUserOrThrow(username);
        return friendshipRepository.findByRecipientAndStatus(user, FriendshipStatus.PENDING);
    }

    @Override
    public List<Friendship> getSentRequests(String username) {
        User user = getUserOrThrow(username);
        return friendshipRepository.findByRequesterAndStatus(user, FriendshipStatus.PENDING);
    }

    @Override
    public List<FriendSuggestionDto> getSuggestedFriends(String username) {
        // "People you may know": ნებისმიერი დარეგისტრირებული იუზერი, ვინც არ
        // ხარ უკვე მეგობარი და არც pending request გაქვს მასთან (არც
        // გაგზავნილი, არც მიღებული). მეგობრის-მეგობრები მაღლა ჩნდებიან
        // (mutual friend count-ით დალაგებული), დანარჩენები — ქვემოთ.

        List<User> myFriends = getFriends(username);

        // Everyone we should never suggest: ourselves, our existing friends,
        // and anyone we already have a pending request with (sent or received).
        Set<String> excluded = new HashSet<>();
        excluded.add(username);
        myFriends.forEach(friend -> excluded.add(friend.getUsername()));
        getPendingIncomingRequests(username).forEach(req -> excluded.add(req.getRequester().getUsername()));
        getSentRequests(username).forEach(req -> excluded.add(req.getRecipient().getUsername()));

        // Count how many of our friends are also friends with each candidate.
        Map<String, Integer> mutualCounts = new HashMap<>();
        for (User friend : myFriends) {
            for (User friendOfFriend : getFriends(friend.getUsername())) {
                String candidate = friendOfFriend.getUsername();
                if (!excluded.contains(candidate)) {
                    mutualCounts.merge(candidate, 1, Integer::sum);
                }
            }
        }

        // ყველა დანარჩენი დარეგისტრირებული იუზერი (mutual friend გარეშეც) —
        // 0 mutual-ით ემატება სიას, რომ suggestion-ები ცარიელი აღარ დარჩეს.
        for (User candidate : userRepository.findAll()) {
            if (!excluded.contains(candidate.getUsername())) {
                mutualCounts.putIfAbsent(candidate.getUsername(), 0);
            }
        }

        return mutualCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new FriendSuggestionDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private Friendship getFriendshipOrThrow(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Friend request not found."));
    }
}
