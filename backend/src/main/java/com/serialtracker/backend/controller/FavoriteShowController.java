package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.FavoriteShow;
import com.serialtracker.backend.repository.FavoriteShowRepository;
import com.serialtracker.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "http://localhost:5173")
public class FavoriteShowController {

    public static final int MAX_FAVORITES = 5;

    private final FavoriteShowRepository favoriteRepository;
    private final UserRepository userRepository;

    public FavoriteShowController(FavoriteShowRepository favoriteRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    private Long userIdOf(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

    // ამ იუზერის favorite შოუების id-ები (დამატების რიგით)
    @GetMapping
    public ResponseEntity<?> getFavorites(@RequestParam String username) {
        Long userId = userIdOf(username);
        List<Integer> showIds = favoriteRepository.findByUserIdOrderByIdAsc(userId)
                .stream().map(FavoriteShow::getShowId).toList();
        return ResponseEntity.ok(showIds);
    }

    // დამატება — მაქს. 5, დუბლიკატის გარეშე
    @PostMapping
    public ResponseEntity<?> addFavorite(@RequestParam String username, @RequestParam int showId) {
        Long userId = userIdOf(username);

        if (favoriteRepository.findByUserIdAndShowId(userId, showId).isPresent()) {
            return ResponseEntity.badRequest().body("This show is already in your favorites.");
        }
        if (favoriteRepository.countByUserId(userId) >= MAX_FAVORITES) {
            return ResponseEntity.badRequest().body("You can only have " + MAX_FAVORITES + " favorite TV shows.");
        }

        favoriteRepository.save(new FavoriteShow(userId, showId));

        List<Integer> showIds = favoriteRepository.findByUserIdOrderByIdAsc(userId)
                .stream().map(FavoriteShow::getShowId).toList();
        return ResponseEntity.ok(showIds);
    }

    // ამოშლა
    @DeleteMapping
    public ResponseEntity<?> removeFavorite(@RequestParam String username, @RequestParam int showId) {
        Long userId = userIdOf(username);
        favoriteRepository.findByUserIdAndShowId(userId, showId)
                .ifPresent(favoriteRepository::delete);

        List<Integer> showIds = favoriteRepository.findByUserIdOrderByIdAsc(userId)
                .stream().map(FavoriteShow::getShowId).toList();
        return ResponseEntity.ok(showIds);
    }
}
