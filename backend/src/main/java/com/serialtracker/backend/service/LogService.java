package com.serialtracker.backend.service;

import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LogService {

    private final UserRepository userRepository;
    private final UserShowStatusRepository showStatusRepository;
    private final UserEpisodeStatusRepository episodeStatusRepository;

    public LogService(UserRepository userRepository,
                      UserShowStatusRepository showStatusRepository,
                      UserEpisodeStatusRepository episodeStatusRepository) {
        this.userRepository = userRepository;
        this.showStatusRepository = showStatusRepository;
        this.episodeStatusRepository = episodeStatusRepository;
    }

    public void saveLog(LogRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUsername()));
        Long userId = user.getId();

        // 1) show-level: rating / review / liked / status / rewatch / watchDate
        UserShowStatus status = showStatusRepository
                .findByUserIdAndShowId(userId, req.getShowId())
                .orElse(new UserShowStatus(userId, req.getShowId(), SeriesStatus.WATCHING));

        if (req.getRating() > 0) status.setRating(req.getRating());
        if (req.getReview() != null) status.setReview(req.getReview());
        if (req.getLiked() != null) status.setFavorite(req.getLiked());

        // 🌟 ახალი: rewatch + watchDate
        if (req.getRewatch() != null) status.setRewatch(req.getRewatch());
        if (req.getWatchDate() != null && !req.getWatchDate().isEmpty()) {
            status.setWatchDate(LocalDate.parse(req.getWatchDate()));
        }

        // 🌟 სტატუსის ლოგიკა
        if (Boolean.TRUE.equals(req.getWholeShow())) {
            status.setStatus(SeriesStatus.COMPLETED);   // მთელი სერიალი ნანახია
        } else if (req.getSeasonNumber() != null && req.getEpisodeNumber() != null) {
            status.setStatus(SeriesStatus.WATCHING);     // კონკრეტული ეპიზოდი — ჯერ უყურებს
        }

        showStatusRepository.save(status);

        // 2) episode-level: ნანახი ეპიზოდი (მხოლოდ თუ კონკრეტული ეპიზოდია, არა Whole Show)
        if (!Boolean.TRUE.equals(req.getWholeShow())
                && req.getSeasonNumber() != null && req.getEpisodeNumber() != null) {
            boolean exists = episodeStatusRepository
                    .existsByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(
                            userId, req.getShowId(), req.getSeasonNumber(), req.getEpisodeNumber());
            if (!exists) {
                episodeStatusRepository.save(new UserEpisodeStatus(
                        userId, req.getShowId(), req.getSeasonNumber(), req.getEpisodeNumber()));
            }
        }
    }
}