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

        boolean isWholeShow = Boolean.TRUE.equals(req.getWholeShow());
        boolean isEpisode = !isWholeShow
                && req.getSeasonNumber() != null && req.getEpisodeNumber() != null;

        // ─────────────────────────────────────────────
        // A) კონკრეტული ეპიზოდი — რივიუ/რეიტინგი ეპიზოდზე
        // ─────────────────────────────────────────────
        if (isEpisode) {
            // 1) ჯერ შოუს სტატუსი → WATCHING (მხოლოდ status, რივიუ/რეიტინგი არა)
            UserShowStatus showStatus = showStatusRepository
                    .findByUserIdAndShowId(userId, req.getShowId())
                    .orElse(new UserShowStatus(userId, req.getShowId(), SeriesStatus.WATCHING));
            showStatus.setStatus(SeriesStatus.WATCHING);
            showStatusRepository.save(showStatus);

            // 2) რივიუ/რეიტინგი/like/rewatch/watchDate → ეპიზოდზე
            UserEpisodeStatus ep = episodeStatusRepository
                    .findByUserIdAndShowIdAndSeasonNumberAndEpisodeNumber(
                            userId, req.getShowId(), req.getSeasonNumber(), req.getEpisodeNumber())
                    .orElse(new UserEpisodeStatus(
                            userId, req.getShowId(), req.getSeasonNumber(), req.getEpisodeNumber()));

            if (req.getRating() > 0) ep.setRating(req.getRating());
            if (req.getReview() != null) ep.setReview(req.getReview());
            if (req.getLiked() != null) ep.setLiked(req.getLiked());
            if (req.getRewatch() != null) ep.setRewatch(req.getRewatch());
            if (req.getWatchDate() != null && !req.getWatchDate().isEmpty()) {
                ep.setWatchDate(LocalDate.parse(req.getWatchDate()));
            }

            episodeStatusRepository.save(ep);
            return;
        }

        // ─────────────────────────────────────────────
        // B) Whole Show (ან სეზონი/ეპიზოდი არ აირჩა) — რივიუ/რეიტინგი შოუზე
        // ─────────────────────────────────────────────
        UserShowStatus status = showStatusRepository
                .findByUserIdAndShowId(userId, req.getShowId())
                .orElse(new UserShowStatus(userId, req.getShowId(), SeriesStatus.WATCHING));

        if (req.getRating() > 0) status.setRating(req.getRating());
        if (req.getReview() != null) status.setReview(req.getReview());
        if (req.getLiked() != null) status.setFavorite(req.getLiked());
        if (req.getRewatch() != null) status.setRewatch(req.getRewatch());
        if (req.getWatchDate() != null && !req.getWatchDate().isEmpty()) {
            status.setWatchDate(LocalDate.parse(req.getWatchDate()));
        }

        if (isWholeShow) {
            status.setStatus(SeriesStatus.COMPLETED);
        }

        showStatusRepository.save(status);
    }
}