package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.MovieListItem;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.dto.MovieListDto;
import com.serialtracker.backend.dto.MovieListItemDto;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.service.MovieListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class MovieListController {

    // TODO: same placeholder situation as FriendController — "actingUsername"
    // should eventually come from the authenticated user (@AuthenticationPrincipal)
    // once a JWT filter reads the Authorization header. Not secure yet, local-testing only.

    private final MovieListService movieListService;
    private final UserRepository userRepository;

    public MovieListController(MovieListService movieListService, UserRepository userRepository) {
        this.movieListService = movieListService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createList(@RequestParam String actingUsername,
                                         @RequestParam String name,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(defaultValue = "true") boolean isPublic) {
        try {
            MovieList list = movieListService.createList(actingUsername, name, description, isPublic);
            return ResponseEntity.ok(MovieListDto.from(list, actingUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{listId}")
    public ResponseEntity<?> renameList(@PathVariable Long listId,
                                         @RequestParam String actingUsername,
                                         @RequestParam String name,
                                         @RequestParam(required = false) String description) {
        try {
            MovieList list = movieListService.renameList(listId, actingUsername, name, description);
            return ResponseEntity.ok(MovieListDto.from(list, actingUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{listId}/visibility")
    public ResponseEntity<?> setVisibility(@PathVariable Long listId,
                                            @RequestParam String actingUsername,
                                            @RequestParam boolean isPublic) {
        try {
            MovieList list = movieListService.setVisibility(listId, actingUsername, isPublic);
            return ResponseEntity.ok(MovieListDto.from(list, actingUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<?> deleteList(@PathVariable Long listId,
                                         @RequestParam String actingUsername) {
        try {
            movieListService.deleteList(listId, actingUsername);
            return ResponseEntity.ok("List deleted.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{listId}/shows")
    public ResponseEntity<?> addShow(@PathVariable Long listId,
                                      @RequestParam String actingUsername,
                                      @RequestParam int showId) {
        try {
            MovieListItem item = movieListService.addShow(listId, actingUsername, showId);
            return ResponseEntity.ok(MovieListItemDto.from(item));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{listId}/shows/{showId}")
    public ResponseEntity<?> removeShow(@PathVariable Long listId,
                                         @PathVariable int showId,
                                         @RequestParam String actingUsername) {
        try {
            movieListService.removeShow(listId, actingUsername, showId);
            return ResponseEntity.ok("Show removed from list.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{listId}/order")
    public ResponseEntity<?> reorderShows(@PathVariable Long listId,
                                           @RequestParam String actingUsername,
                                           @RequestBody List<Integer> orderedShowIds) {
        try {
            movieListService.reorderShows(listId, actingUsername, orderedShowIds);
            return ResponseEntity.ok("List reordered.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyLists(@RequestParam String actingUsername) {
        try {
            List<MovieListDto> lists = movieListService.getListsOwnedBy(actingUsername).stream()
                    .map(list -> MovieListDto.from(list, actingUsername))
                    .toList();
            return ResponseEntity.ok(lists);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Newest public lists from any user, used for the Lists page's
    // "Featured Lists" section. No actingUsername needed since this is
    // just a public, read-only feed.
    @GetMapping("/public")
    public ResponseEntity<?> getFeaturedLists() {
        List<MovieListDto> lists = movieListService.getRecentPublicLists().stream()
                .map(list -> MovieListDto.from(list, usernameOf(list.getOwnerId())))
                .toList();
        return ResponseEntity.ok(lists);
    }

    // Every public list, newest first - backs the "View All" page for
    // Featured Lists (unlike /public, which is capped to a handful).
    @GetMapping("/public/all")
    public ResponseEntity<?> getAllFeaturedLists() {
        List<MovieListDto> lists = movieListService.getAllPublicLists().stream()
                .map(list -> MovieListDto.from(list, usernameOf(list.getOwnerId())))
                .toList();
        return ResponseEntity.ok(lists);
    }

    // Public lists with the most shows in them, used for the Lists page's
    // "Popular This Week" section. No real popularity tracking exists yet,
    // so this approximates it as "biggest public lists".
    @GetMapping("/public/popular")
    public ResponseEntity<?> getPopularLists() {
        List<MovieListDto> lists = movieListService.getPopularPublicLists().stream()
                .map(list -> MovieListDto.from(list, usernameOf(list.getOwnerId())))
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{listId}")
    public ResponseEntity<?> getList(@PathVariable Long listId) {
        try {
            MovieList list = movieListService.getList(listId);
            String ownerUsername = usernameOf(list.getOwnerId());

            List<MovieListItemDto> items = movieListService.getItems(listId).stream()
                    .map(MovieListItemDto::from)
                    .toList();

            return ResponseEntity.ok(MovieListDto.from(list, ownerUsername, items));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String usernameOf(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("unknown");
    }
}
