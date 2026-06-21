package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.service.FriendService;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.dto.FriendshipDto;
import com.serialtracker.backend.dto.FriendSuggestionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    // TODO: every "actingUsername" parameter below is a placeholder.
    // It should come from the authenticated user (e.g. via @AuthenticationPrincipal)
    // once a JWT validation filter exists in SecurityConfig. Right now there is no
    // filter that reads the Authorization header, so the caller has to tell us
    // who they are directly. This is NOT secure and is only meant for local testing
    // until the auth feature is finished.

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestParam String actingUsername,
                                          @RequestParam String targetUsername) {
        try {
            Friendship friendship = friendService.sendRequest(actingUsername, targetUsername);
            return ResponseEntity.ok(FriendshipDto.from(friendship));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long friendshipId,
                                            @RequestParam String actingUsername) {
        try {
            Friendship friendship = friendService.acceptRequest(friendshipId, actingUsername);
            return ResponseEntity.ok(FriendshipDto.from(friendship));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{friendshipId}/decline")
    public ResponseEntity<?> declineRequest(@PathVariable Long friendshipId,
                                             @RequestParam String actingUsername) {
        try {
            Friendship friendship = friendService.declineRequest(friendshipId, actingUsername);
            return ResponseEntity.ok(FriendshipDto.from(friendship));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> removeFriend(@RequestParam String actingUsername,
                                           @RequestParam String targetUsername) {
        try {
            friendService.removeFriend(actingUsername, targetUsername);
            return ResponseEntity.ok("Friend removed.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getFriends(@RequestParam String actingUsername) {
        try {
            List<String> usernames = friendService.getFriends(actingUsername)
                    .stream()
                    .map(User::getUsername)
                    .toList();
            return ResponseEntity.ok(usernames);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingIncomingRequests(@RequestParam String actingUsername) {
        try {
            List<FriendshipDto> pending = friendService.getPendingIncomingRequests(actingUsername)
                    .stream()
                    .map(FriendshipDto::from)
                    .toList();
            return ResponseEntity.ok(pending);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentRequests(@RequestParam String actingUsername) {
        try {
            List<FriendshipDto> sent = friendService.getSentRequests(actingUsername)
                    .stream()
                    .map(FriendshipDto::from)
                    .toList();
            return ResponseEntity.ok(sent);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getSuggestedFriends(@RequestParam String actingUsername) {
        try {
            List<FriendSuggestionDto> suggestions = friendService.getSuggestedFriends(actingUsername);
            return ResponseEntity.ok(suggestions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
