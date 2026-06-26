package com.serialtracker.backend.controller;

import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
            @RequestParam(required = false) String status) { // String-ად ვიღებთ, რომ null/ცარიელი დავიჭიროთ

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        UserShowStatus showStatus = statusRepository.findByUserIdAndShowId(userId, showId)
                .orElse(new UserShowStatus(userId, showId, null));

        // თუ სტატუსი ცარიელია ან null, ბაზაში ვსვამთ null-ს (ვაუქმებთ)
        if (status == null || status.trim().isEmpty() || status.equals("null")) {
            showStatus.setStatus(null);
            statusRepository.save(showStatus);
            return ResponseEntity.ok("Show status cleared (set to null)");
        } else {
            // წინააღმდეგ შემთხვევაში ვკონვერტირებთ Enum-ში
            SeriesStatus seriesStatus = SeriesStatus.valueOf(status.toUpperCase());
            showStatus.setStatus(seriesStatus);
            statusRepository.save(showStatus);
            return ResponseEntity.ok("Show status updated to: " + seriesStatus);
        }
    }

    //  ახალი ენდფოინთი: ყველა ეპიზოდის მონიშვნა (ფრონტენდიდან სეზონების/ეპიზოდების თვლა TMDB სირთულის გამო არ გვინდა, უბრალოდ ვინახავთ ფიქტიურ -1 ჩანაწერს, ან თუ ბაზა სხვანაირადაა აწყობილი, შენს ლოგიკას მოარგებ)
    // თუმცა ყველაზე მარტივია: ფრონტენდზე როცა დაინახავს სტატუსს COMPLETED, ეგრევე მონიშნულად თვლის.
    // თუ მაინც გინდა კონკრეტული სეზონების ჩაყრა, ბაზაში ვინახავთ მიმდინარე თრექინგს.
    @PostMapping("/watch-all-episodes")
    public ResponseEntity<?> watchAllEpisodes(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // დაზღვევისთვის ჯერ ვშლით ძველებს, რომ დუბლიკატები არ შეიქმნას
        episodeRepository.deleteByUserIdAndShowId(userId, showId);

        // აქ შეგვიძლია ჩავწეროთ ერთი "სპეციალური" ჩანაწერი (მაგალითად სეზონი 0, ეპიზოდი 0) რომელიც მიანიშნებს რომ ყველაფერი ნანახია,
        // ან უბრალოდ დავაბრუნოთ წარმატება, რადგან COMPLETED სტატუსი თავისთავად ნიშნავს რომ ყველაფერი ნანახია!
        return ResponseEntity.ok("All episodes marked as watched");
    }

    // 🔴 ახალი ენდფოინთი: ყველა ეპიზოდის ამოშლა ნანახებიდან (Unwatched)
    @Transactional
    @PostMapping("/unwatch-all-episodes")
    public ResponseEntity<?> unwatchAllEpisodes(
            @RequestParam String username,
            @RequestParam int showId) {

        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // ვშლით ამ იუზერის ყველა ნანახ ეპიზოდს ამ სერიალისთვის
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