package com.serialtracker.backend.service;

import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.entity.SeriesStatus;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.entity.Activity;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import com.serialtracker.backend.repository.ActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LogService {

    private final UserRepository userRepository;
    private final UserShowStatusRepository showStatusRepository;
    private final UserEpisodeStatusRepository episodeStatusRepository;
    private final ActivityRepository activityRepository;

    public LogService(UserRepository userRepository,
                      UserShowStatusRepository showStatusRepository,
                      UserEpisodeStatusRepository episodeStatusRepository,
                      ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.showStatusRepository = showStatusRepository;
        this.episodeStatusRepository = episodeStatusRepository;
        this.activityRepository = activityRepository;
    }

    public void saveLog(LogRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUsername()));
        Long userId = user.getId();

        boolean isWholeShow = Boolean.TRUE.equals(req.getWholeShow());
        boolean isEpisode = !isWholeShow
                && req.getSeasonNumber() != null && req.getEpisodeNumber() != null;

        String title = (req.getShowName() != null) ? req.getShowName() : "Show";

        // ─────────────────────────────────────────────
        // A) კონკრეტული ეპიზოდი
        // ─────────────────────────────────────────────
        if (isEpisode) {
            UserShowStatus showStatus = showStatusRepository
                    .findByUserIdAndShowId(userId, req.getShowId())
                    .orElse(new UserShowStatus(userId, req.getShowId(), SeriesStatus.WATCHING));

            showStatus.setStatus(SeriesStatus.WATCHING);
            if (req.getRewatch() != null) showStatus.setRewatch(req.getRewatch());
            if (req.getWatchDate() != null && !req.getWatchDate().isEmpty()) {
                showStatus.setWatchDate(LocalDate.parse(req.getWatchDate()));
            }
            showStatusRepository.save(showStatus);

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

            // 🟢 აქტივობის ჩაწერა ეპიზოდისთვის
            String detail = "Watched S" + req.getSeasonNumber() + " E" + req.getEpisodeNumber();
            if (req.getRating() > 0) detail += " • Rated " + req.getRating() + "/5";

            activityRepository.save(new Activity(req.getUsername(), req.getShowId(), title, "WATCHED_EPISODE", detail));
            return;
        }

        // ─────────────────────────────────────────────
        // B) Whole Show — ყველაფერი შოუზე
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

        // 🟢 აქტივობის ჩაწერა მთლიანი შოუსთვის (გაერთიანების ლოგიკით)
        String action = isWholeShow ? "COMPLETED" : "LOGGED";
        String detail = isWholeShow ? "Completed the whole show" : "Logged show details";
        if (req.getRating() > 0) detail += " • Rated " + req.getRating() + "/5";
        if (Boolean.TRUE.equals(req.getLiked())) detail += " • ❤️";

        // 🔍 1. წამოვიღოთ იუზერის ბოლო აქტივობები დუბლიკატის შესამოწმებლად
        List<Activity> existingActivities = activityRepository.findByUsernameOrderByCreatedAtDesc(req.getUsername());
        Activity actToSave = null;

        if (!existingActivities.isEmpty() && existingActivities.get(0).getShowId() == req.getShowId()) {
            actToSave = existingActivities.get(0); // თუ ბოლო აქტივობა ამავე შოუზეა, ჩავასწოროთ
        } else {
            actToSave = new Activity(); // სხვა შემთხვევაში ახალს ვქმნით
            actToSave.setUsername(req.getUsername());
            actToSave.setShowId(req.getShowId());
            actToSave.setShowName(title);
        }

        // ✍️ 2. მონაცემების განახლება
        actToSave.setActionType(action);
        actToSave.setDetail(detail);
        actToSave.setCreatedAt(java.time.LocalDateTime.now());

        if (req.getRating() > 0) {
            actToSave.setRating((double) req.getRating());
        }

        // 🟢 3. პოსტერის აღდგენა/დაზღვევა
        if (req.getPosterPath() != null && !req.getPosterPath().trim().isEmpty() && !req.getPosterPath().equals("null")) {
            actToSave.setPosterPath(req.getPosterPath());
        } else if (actToSave.getPosterPath() == null) {
            // თუ ფრონტიდან არ მოვიდა, ისტორიიდან ამოვიღოთ წინა ჩანაწერის ფოტო
            String foundPoster = existingActivities.stream()
                    .filter(a -> a.getShowId() == req.getShowId() && a.getPosterPath() != null)
                    .map(Activity::getPosterPath)
                    .findFirst()
                    .orElse(null);
            if (foundPoster != null) actToSave.setPosterPath(foundPoster);
        }

        activityRepository.save(actToSave);
    }
}