package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "http://localhost:5173")
public class UserTrackingController {

    private final UserShowStatusRepository statusRepository;
    private final UserEpisodeStatusRepository episodeRepository;
    private final UserRepository userRepository;

    public UserTrackingController(UserShowStatusRepository statusRepository,
                                  UserEpisodeStatusRepository episodeRepository,
                                  UserRepository userRepository) {
        this.statusRepository = statusRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/show-status")
    public ResponseEntity<?> updateShowStatus(
            @RequestParam String username,
            @RequestParam int showId,
            @RequestParam SeriesStatus status) {


        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        UserShowStatus showStatus = statusRepository.findByUserIdAndShowId(userId, showId)
                .orElse(new UserShowStatus(userId, showId, status));

        showStatus.setStatus(status);
        statusRepository.save(showStatus);
        return ResponseEntity.ok("Show status updated to: " + status);
    }

    @PostMapping("/toggle-episode")
    public ResponseEntity<?> toggleEpisode(
            @RequestParam String username,
            @RequestParam int showId,
            @RequestParam int seasonNumber,
            @RequestParam int episodeNumber) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        var existing = episodeRepository.findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(
                userId, showId, seasonNumber, episodeNumber);

        if (existing.isPresent()) {
            episodeRepository.delete(existing.get());
            return ResponseEntity.ok("Episode marked as UNWATCHED");
        } else {
            episodeRepository.save(new UserEpisodeStatus(userId, showId, seasonNumber, episodeNumber));
            return ResponseEntity.ok("Episode marked as WATCHED");
        }
    }

    @GetMapping("/watched-episodes")
    public ResponseEntity<List<UserEpisodeStatus>> getWatchedEpisodes(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        return ResponseEntity.ok(episodeRepository.findByUserIdAndShowId(userId, showId));
    }

    @GetMapping("/get-status")
    public ResponseEntity<?> getShowStatus(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        return ResponseEntity.ok(statusRepository.findByUserIdAndShowId(userId, showId).orElse(null));
    }

    @PostMapping("/toggle-favorite")
    public ResponseEntity<?> toggleFavorite(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        UserShowStatus showStatus = statusRepository.findByUserIdAndShowId(userId, showId)
                .orElse(new UserShowStatus(userId, showId, null));


        showStatus.setFavorite(!showStatus.isFavorite());
        statusRepository.save(showStatus);

        return ResponseEntity.ok(showStatus.isFavorite());
    }


}