package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.Recommendation;
import com.serialtracker.backend.dto.DiaryEntryResponse;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import com.serialtracker.backend.repository.RecommendationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "http://localhost:5173")
@Transactional
public class UserTrackingController {

    private final UserShowStatusRepository statusRepository;
    private final UserEpisodeStatusRepository episodeRepository;
    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;

    public UserTrackingController(UserShowStatusRepository statusRepository,
                                  UserEpisodeStatusRepository episodeRepository,
                                  UserRepository userRepository,
                                  RecommendationRepository recommendationRepository) {
        this.statusRepository = statusRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @PostMapping("/show-status")
    public ResponseEntity<?> updateShowStatus(
            @RequestParam String username,
            @RequestParam int showId,
            @RequestParam(required = false) String status) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        UserShowStatus showStatus = statusRepository.findByUserIdAndShowId(userId, showId)
                .orElseGet(() -> {
                    UserShowStatus newStatusObj = new UserShowStatus();
                    newStatusObj.setUserId(userId);
                    newStatusObj.setShowId(showId);
                    newStatusObj.setFavorite(false);
                    return newStatusObj;
                });

        if (status == null || status.trim().isEmpty() || status.equals("null")) {
            showStatus.setStatus(null);
            statusRepository.save(showStatus);
            return ResponseEntity.ok("Show status cleared (set to null)");
        } else {
            SeriesStatus seriesStatus = SeriesStatus.valueOf(status.toUpperCase());
            showStatus.setStatus(seriesStatus);
            statusRepository.save(showStatus);
            return ResponseEntity.ok("Show status updated to: " + seriesStatus);
        }
    }

    @PostMapping("/watch-all-episodes")
    public ResponseEntity<?> watchAllEpisodes(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        episodeRepository.deleteByUserIdAndShowId(userId, showId);
        return ResponseEntity.ok("All episodes marked as watched");
    }

    @PostMapping("/unwatch-all-episodes")
    public ResponseEntity<?> unwatchAllEpisodes(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        episodeRepository.deleteByUserIdAndShowId(userId, showId);
        return ResponseEntity.ok("All episodes marked as unwatched");
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

    @GetMapping("/watchlist")
    public ResponseEntity<?> getWatchlist(@RequestParam String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        List<Integer> showIds = statusRepository.findByUserIdAndStatusOrderByIdDesc(userId, SeriesStatus.PLAN_TO_WATCH)
                .stream()
                .map(UserShowStatus::getShowId)
                .toList();

        return ResponseEntity.ok(showIds);
    }

    // Diary: ამ იუზერის ყველა დათარიღებული ჩანაწერი (ეპიზოდები + whole-show),
    // watchDate-ით ახლიდან-ძველისკენ დალაგებული. title/poster front-end-ი TMDB-დან იღებს.
    @GetMapping("/diary")
    public ResponseEntity<?> getDiary(@RequestParam String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        List<DiaryEntryResponse> entries = new ArrayList<>();

        for (UserEpisodeStatus ep : episodeRepository.findByUserIdAndWatchDateIsNotNull(userId)) {
            DiaryEntryResponse d = new DiaryEntryResponse();
            d.setShowId(ep.getShowId());
            d.setSeasonNumber(ep.getSeasonNumber());
            d.setEpisodeNumber(ep.getEpisodeNumber());
            d.setWatchDate(ep.getWatchDate());
            d.setRating(ep.getRating());
            d.setLiked(ep.isLiked());
            d.setRewatch(ep.isRewatch());
            d.setReview(ep.getReview());
            d.setWholeShow(false);
            entries.add(d);
        }

        for (UserShowStatus s : statusRepository.findByUserIdAndWatchDateIsNotNull(userId)) {
            DiaryEntryResponse d = new DiaryEntryResponse();
            d.setShowId(s.getShowId());
            d.setSeasonNumber(null);
            d.setEpisodeNumber(null);
            d.setWatchDate(s.getWatchDate());
            d.setRating(s.getRating());
            d.setLiked(s.isFavorite());
            d.setRewatch(s.isRewatch());
            d.setReview(s.getReview());
            d.setWholeShow(true);
            entries.add(d);
        }

        entries.sort(Comparator.comparing(DiaryEntryResponse::getWatchDate).reversed());
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/toggle-favorite")
    public ResponseEntity<?> toggleFavorite(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        UserShowStatus showStatus = statusRepository.findByUserIdAndShowId(userId, showId)
                .orElseGet(() -> {
                    UserShowStatus newStatusObj = new UserShowStatus();
                    newStatusObj.setUserId(userId);
                    newStatusObj.setShowId(showId);
                    newStatusObj.setStatus(null);
                    newStatusObj.setFavorite(false);
                    return newStatusObj;
                });

        showStatus.setFavorite(!showStatus.isFavorite());
        statusRepository.save(showStatus);

        return ResponseEntity.ok(showStatus.isFavorite());
    }

    // ==========================================
    // ✉️ RECOMMENDATIONS FUNCTIONALITY
    // ==========================================

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendShow(
            @RequestParam String senderUsername,
            @RequestParam String targetUsername,
            @RequestParam int showId,
            @RequestParam String showName,
            @RequestParam(required = false) String comment) {

        boolean alreadyRecommended = recommendationRepository.existsBySenderUsernameAndTargetUsernameAndShowId(senderUsername, targetUsername, showId);
        if (alreadyRecommended) {
            return ResponseEntity.badRequest().body("You have already recommended this show to this friend!");
        }

        Recommendation rec = new Recommendation(senderUsername, targetUsername, showId, showName, comment);
        recommendationRepository.save(rec);
        return ResponseEntity.ok("Recommendation sent successfully!");
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestParam String username) {
        List<Recommendation> recs = recommendationRepository.findByTargetUsernameOrderByCreatedAtDesc(username);
        return ResponseEntity.ok(recs);
    }

    @PostMapping("/recommendations/read")
    public ResponseEntity<?> markAsRead(@RequestParam Long id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        rec.setRead(true); // ან rec.setIsRead(true); გააჩნია როგორ გიწერია ენთითიში
        recommendationRepository.save(rec);
        return ResponseEntity.ok("Notification marked as read");
    }

    @GetMapping("/recommendations/sent")
    public ResponseEntity<?> getSentRecommendations(@RequestParam String username) {
        List<Recommendation> sentRecs = recommendationRepository.findBySenderUsername(username);
        return ResponseEntity.ok(sentRecs);
    }

    @GetMapping("/recommendations/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestParam String username) {
        int count = recommendationRepository.findByTargetUsernameAndIsReadFalse(username).size();
        return ResponseEntity.ok(count);
    }
}