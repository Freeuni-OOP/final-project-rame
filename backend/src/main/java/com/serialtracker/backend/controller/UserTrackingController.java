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
import com.serialtracker.backend.entity.Activity;
import com.serialtracker.backend.repository.*;
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
    private final ActivityRepository activityRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    public UserTrackingController(UserShowStatusRepository statusRepository,
                                  UserEpisodeStatusRepository episodeRepository,
                                  UserRepository userRepository,
                                  RecommendationRepository recommendationRepository,
                                  ActivityRepository activityRepository,
                                  ReviewLikeRepository reviewLikeRepository) {
        this.statusRepository = statusRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
        this.activityRepository = activityRepository;
        this.reviewLikeRepository = reviewLikeRepository;
    }

    @PostMapping("/show-status")
    public ResponseEntity<?> updateShowStatus(
            @RequestParam String username,
            @RequestParam int showId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String showName,
            @RequestParam(required = false) String posterPath,
            @RequestParam(required = false) Double rating) {

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

            String finalShowName = "Show #" + showId;
            if (showName != null && !showName.trim().isEmpty() && !showName.equals("null") && !showName.equals("undefined")) {
                finalShowName = showName;
            }

            // 🟢 ყოველთვის ვქმნით ახალ აქტივობას სუფთად, რათა სხვადასხვა ქმედება ერთმანეთს არ დაეტაკოს
            List<Activity> existingActivities = activityRepository.findByUsernameOrderByCreatedAtDesc(username);
            Activity actToSave = new Activity();
            actToSave.setUsername(username);
            actToSave.setShowId(showId);
            actToSave.setShowName(finalShowName);

            // 🟢 ვსვამთ ActionType-ს, რომ ფრონტზე ადვილად წავიკითხოთ სტატუსი
            actToSave.setActionType(seriesStatus.toString()); // WATCHING, COMPLETED, DROPPED, PLAN_TO_WATCH
            actToSave.setDetail("Changed status to " + seriesStatus.toString().replace('_', ' '));
            actToSave.setCreatedAt(java.time.LocalDateTime.now());

            // 🟢 პოსტერის დაზღვევა
            if (posterPath != null && !posterPath.trim().isEmpty() && !posterPath.equals("null")) {
                actToSave.setPosterPath(posterPath);
            } else {
                // თუ ფრონტიდან არ მოვიდა, ვეძებთ იუზერის ძველ ჩანაწერებში ამ showId-ზე ფოტოს აღსადგენად
                String foundPoster = existingActivities.stream()
                        .filter(a -> a.getShowId() == showId && a.getPosterPath() != null)
                        .map(Activity::getPosterPath)
                        .findFirst()
                        .orElse(null);
                if (foundPoster != null) actToSave.setPosterPath(foundPoster);
            }

            if (rating != null) {
                actToSave.setRating(rating);
            }

            activityRepository.save(actToSave);
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
    public ResponseEntity<?> getDiary(@RequestParam String username,
                                      @RequestParam(required = false) String viewer) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // მნახველის ლაიქები (TYPE:id) — likedByMe-სთვის
        java.util.Set<String> myLikedKeys = new java.util.HashSet<>();
        if (viewer != null) {
            userRepository.findByUsername(viewer).ifPresent(v ->
                    reviewLikeRepository.findByLikerUserId(v.getId())
                            .forEach(rl -> myLikedKeys.add(rl.getReviewType() + ":" + rl.getReviewId())));
        }

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
            applyDiaryLikes(d, "EPISODE", ep.getId(), myLikedKeys);
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
            applyDiaryLikes(d, "SHOW", s.getId(), myLikedKeys);
            entries.add(d);
        }

        entries.sort(Comparator.comparing(DiaryEntryResponse::getWatchDate).reversed());
        return ResponseEntity.ok(entries);
    }

    private void applyDiaryLikes(DiaryEntryResponse d, String type, Long reviewId, java.util.Set<String> myLikedKeys) {
        d.setReviewId(reviewId);
        d.setReviewType(type);
        d.setLikeCount(reviewLikeRepository.countByReviewTypeAndReviewId(type, reviewId));
        d.setLikedByMe(myLikedKeys.contains(type + ":" + reviewId));
    }

    @PostMapping("/toggle-favorite")
    public ResponseEntity<?> toggleFavorite(
            @RequestParam String username,
            @RequestParam int showId,
            @RequestParam(required = false) String showName,
            @RequestParam(required = false) String posterPath) { // 🟢 ჩამატებულია posterPath

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

        if (showStatus.isFavorite()) {
            String finalShowName = (showName != null && !showName.isEmpty()) ? showName : "Show #" + showId;

            // 🔍 უსაფრთხო ძებნა: ვნახულობთ, ბოლო აქტივობა ხომ არ არის ამავე სერიალზე
            List<Activity> existingActivities = activityRepository.findByUsernameOrderByCreatedAtDesc(username);
            Activity actToSave = null;

            if (!existingActivities.isEmpty() && existingActivities.get(0).getShowId() == showId) {
                // თუ ბოლო ჩანაწერი ამავე სერიალზეა, ახალს კი არ ვქმნით, იმავეს ვაახლებთ!
                actToSave = existingActivities.get(0);
            } else {
                // თუ სხვა სერიალია ან ფიდი ცარიელია, ვქმნით ახალს
                actToSave = new Activity();
                actToSave.setUsername(username);
                actToSave.setShowId(showId);
                actToSave.setShowName(finalShowName);
            }

            // ✍️ მონაცემების დასმა
            actToSave.setActionType("LIKED");
            actToSave.setDetail("Added to favorites ❤️");
            actToSave.setCreatedAt(java.time.LocalDateTime.now()); // დროს ვწევთ წინ

            // 🟢 პოსტერის აღდგენა/დაზღვევა
            if (posterPath != null && !posterPath.trim().isEmpty() && !posterPath.equals("null")) {
                actToSave.setPosterPath(posterPath);
            } else if (actToSave.getPosterPath() == null) {
                // თუ ფრონტიდან არ მოვიდა, ვეძებთ ისტორიაში ამავე სერიალის ძველ ფოტოს
                String foundPoster = existingActivities.stream()
                        .filter(a -> a.getShowId() == showId && a.getPosterPath() != null)
                        .map(Activity::getPosterPath)
                        .findFirst()
                        .orElse(null);
                if (foundPoster != null) actToSave.setPosterPath(foundPoster);
            }

            activityRepository.save(actToSave);
        }

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

        rec.setRead(true);
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

    // ==========================================
    // 🕒 USER ACTIVITY FEED
    // ==========================================

    @GetMapping("/activity")
    public ResponseEntity<?> getUserActivity(@RequestParam String username) {
        // 🟢 ფრონტენდისთვის ბაზიდან წამოვიღებთ უახლეს აქტივობებს
        List<Activity> activities = activityRepository.findByUsernameOrderByCreatedAtDesc(username);
        return ResponseEntity.ok(activities);
    }
}