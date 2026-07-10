package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.MovieListItem;
import com.serialtracker.backend.entity.MovieListLike;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.dto.MovieListDto;
import com.serialtracker.backend.dto.MovieListItemDto;
import com.serialtracker.backend.repository.MovieListLikeRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.service.MovieListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class MovieListController {

    private final MovieListService movieListService;
    private final UserRepository userRepository;
    private final MovieListLikeRepository likeRepository;

    public MovieListController(MovieListService movieListService,
                               UserRepository userRepository,
                               MovieListLikeRepository likeRepository) {
        this.movieListService = movieListService;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
    }

    @PostMapping
    public ResponseEntity<?> createList(@RequestParam String actingUsername,
                                        @RequestParam String name,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(defaultValue = "true") boolean isPublic) {
        try {
            MovieList list = movieListService.createList(actingUsername, name, description, isPublic);
            User owner = userRepository.findByUsername(actingUsername).orElse(null);
            return ResponseEntity.ok(owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, actingUsername));
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
            return ResponseEntity.ok(owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, actingUsername));
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
            return ResponseEntity.ok(owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, actingUsername));
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
                    User owner = userRepository.findById(list.getOwnerId()).orElse(null);
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/public/all")
    public ResponseEntity<?> getAllFeaturedLists() {
        List<MovieListDto> lists = movieListService.getAllPublicLists().stream()
                .map(list -> {
                    User owner = userRepository.findById(list.getOwnerId()).orElse(null);
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/public/popular")
    public ResponseEntity<?> getPopularLists() {
        List<MovieListDto> lists = movieListService.getPopularPublicLists().stream()
                .map(list -> {
                    User owner = userRepository.findById(list.getOwnerId()).orElse(null);
                    return owner != null ? MovieListDto.from(list, owner) : MovieListDto.from(list, "unknown");
                })
                .toList();
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{listId}")
    public ResponseEntity<?> getList(@PathVariable Long listId,
                                     @RequestParam(required = false) String username) {
        try {
            MovieList list = movieListService.getList(listId);
            User owner = userRepository.findById(list.getOwnerId()).orElse(null);

            List<MovieListItemDto> items = movieListService.getItems(listId).stream()
                    .map(MovieListItemDto::from)
                    .toList();

            MovieListDto dto = owner != null ? MovieListDto.from(list, owner, items) : MovieListDto.from(list, "unknown", items);

            // ლაიქების მონაცემების მიბმა DTO-ზე
            dto.setLikeCount(likeRepository.countByListId(listId));
            if (username != null && !username.isBlank()) {
                boolean likedByMe = userRepository.findByUsername(username)
                        .map(u -> likeRepository.findByLikerUserIdAndListId(u.getId(), listId).isPresent())
                        .orElse(false);
                dto.setLikedByMe(likedByMe);
            }

            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{listId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long listId,
                                        @RequestParam String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        var existing = likeRepository.findByLikerUserIdAndListId(userId, listId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return ResponseEntity.ok(false);
        } else {
            likeRepository.save(new MovieListLike(userId, listId));
            return ResponseEntity.ok(true);
        }
    }

    @GetMapping("/liked")
    public ResponseEntity<?> getRecentlyLiked(@RequestParam String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        List<MovieListDto> liked = likeRepository
                .findByLikerUserIdOrderByLikedAtDesc(userId)
                .stream()
                .map(like -> {
                    MovieList list = movieListService.getList(like.getListId());
                    User owner = userRepository.findById(list.getOwnerId()).orElse(null);

                    // items ჩავრთოთ, რომ პროფილზე სიის პოსტერები დაიხატოს
                    List<MovieListItemDto> items = movieListService.getItems(list.getId()).stream()
                            .map(MovieListItemDto::from)
                            .toList();

                    MovieListDto dto = owner != null
                            ? MovieListDto.from(list, owner, items)
                            : MovieListDto.from(list, "unknown", items);
                    dto.setLikeCount(likeRepository.countByListId(list.getId()));
                    dto.setLikedByMe(true); // რადგან ამ ენდპოინტიდან მიგვაქვს, ესე იგი იუზერს ნამდვილად დალაიქებული აქვს

                    return dto;
                })
                .toList();

        return ResponseEntity.ok(liked);
    }
}