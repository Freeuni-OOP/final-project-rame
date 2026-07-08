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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lists")
@CrossOrigin(origins = "http://localhost:5173")
public class MovieListController {

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
            User owner = userRepository.findByUsername(actingUsername).orElse(null);
            return owner != null ? ResponseEntity.ok(MovieListDto.from(list, owner))
                    : ResponseEntity.ok(MovieListDto.from(list, actingUsername));
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
            User owner = userRepository.findByUsername(actingUsername).orElse(null);
            return owner != null ? ResponseEntity.ok(MovieListDto.from(list, owner))
                    : ResponseEntity.ok(MovieListDto.from(list, actingUsername));
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
            User owner = userRepository.findByUsername(actingUsername).orElse(null);
            return owner != null ? ResponseEntity.ok(MovieListDto.from(list, owner))
                    : ResponseEntity.ok(MovieListDto.from(list, actingUsername));
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
            User owner = userRepository.findByUsername(actingUsername).orElse(null);
            List<MovieListDto> lists = movieListService.getListsOwnedBy(actingUsername).stream()
                    .map(list -> owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, actingUsername))
                    .toList();
            return ResponseEntity.ok(lists);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/public")
    public ResponseEntity<?> getFeaturedLists() {
        List<MovieListDto> lists = movieListService.getRecentPublicLists().stream()
                .map(list -> {
                    User owner = userOf(list.getOwnerId());
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/public/all")
    public ResponseEntity<?> getAllFeaturedLists() {
        List<MovieListDto> lists = movieListService.getAllPublicLists().stream()
                .map(list -> {
                    User owner = userOf(list.getOwnerId());
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/public/popular")
    public ResponseEntity<?> getPopularLists() {
        List<MovieListDto> lists = movieListService.getPopularPublicLists().stream()
                .map(list -> {
                    User owner = userOf(list.getOwnerId());
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{listId}")
    public ResponseEntity<?> getList(@PathVariable Long listId) {
        try {
            MovieList list = movieListService.getList(listId);
            User owner = userOf(list.getOwnerId());

            List<MovieListItemDto> items = movieListService.getItems(listId).stream()
                    .map(MovieListItemDto::from)
                    .toList();

            return owner != null
                    ? ResponseEntity.ok(MovieListDto.from(list, owner, items))
                    : ResponseEntity.ok(MovieListDto.from(list, "unknown", items));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User userOf(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}