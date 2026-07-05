package com.serialtracker.backend.service;

import com.serialtracker.backend.dto.ReviewResponse;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.repository.FriendshipRepository;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewService {

    private final UserRepository userRepository;
    private final UserShowStatusRepository showStatusRepository;
    private final UserEpisodeStatusRepository episodeStatusRepository;
    private final FriendshipRepository friendshipRepository;

    public ReviewService(UserRepository userRepository,
                         UserShowStatusRepository showStatusRepository,
                         UserEpisodeStatusRepository episodeStatusRepository,
                         FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.showStatusRepository = showStatusRepository;
        this.episodeStatusRepository = episodeStatusRepository;
        this.friendshipRepository = friendshipRepository;
    }

    /**
     * ამ შოუს რივიუები, არასავალდებულო სპოილერ-ფილტრით.
     * @param showId          სერიალის TMDB id
     * @param currentUsername ვინ უყურებს გვერდს (მეგობრების გასარჩევად); შეიძლება null
     * @param uptoSeason      მიმდინარე არჩეული სეზონი (null = ფილტრი გამორთულია)
     * @param uptoEpisode     მიმდინარე არჩეული ეპიზოდი (null = ფილტრი გამორთულია)
     */
    public List<ReviewResponse> getReviewsForShow(int showId, String currentUsername,
                                                  Integer uptoSeason, Integer uptoEpisode) {

        // 1) მიმდინარე იუზერის მეგობრების userId-ების ნაკრები
        Set<Long> friendIds = new HashSet<>();
        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(me -> {
                for (Friendship f : friendshipRepository.findAcceptedFriendshipsOf(me)) {
                    // მეორე მხარე (არა მე)
                    Long otherId = f.getRequester().getId().equals(me.getId())
                            ? f.getRecipient().getId()
                            : f.getRequester().getId();
                    friendIds.add(otherId);
                }
            });
        }

        // 2) userId → username რუკა (რომ ყოველ რივიუზე ცალკე მოთხოვნა არ გავუშვათ)
        Map<Long, String> usernameCache = new HashMap<>();

        List<ReviewResponse> result = new ArrayList<>();

        // 3) ეპიზოდების რივიუები
        for (UserEpisodeStatus ep : episodeStatusRepository.findByShowIdAndReviewIsNotNull(showId)) {
            // სპოილერ-ფილტრი: მხოლოდ მიმდინარე სეზონი/ეპიზოდის ჩათვლით და უკან
            if (!passesSpoilerFilter(ep.getSeasonNumber(), ep.getEpisodeNumber(), uptoSeason, uptoEpisode)) {
                continue;
            }
            ReviewResponse r = new ReviewResponse();
            String uname = resolveUsername(ep.getUserId(), usernameCache);
            r.setUsername(uname);
            r.setRating(ep.getRating());
            r.setReview(ep.getReview());
            r.setLiked(ep.isLiked());
            r.setRewatch(ep.isRewatch());
            r.setSeasonNumber(ep.getSeasonNumber());
            r.setEpisodeNumber(ep.getEpisodeNumber());
            r.setFriend(friendIds.contains(ep.getUserId()));
            result.add(r);
        }

        // 4) Whole Show რივიუები (სეზონი/ეპიზოდი არ აქვთ → სპოილერს არ ექვემდებარებიან)
        for (UserShowStatus show : showStatusRepository.findByShowIdAndReviewIsNotNull(showId)) {
            ReviewResponse r = new ReviewResponse();
            String uname = resolveUsername(show.getUserId(), usernameCache);
            r.setUsername(uname);
            r.setRating(show.getRating());
            r.setReview(show.getReview());
            r.setLiked(show.isFavorite());
            r.setRewatch(show.isRewatch());
            r.setSeasonNumber(null);
            r.setEpisodeNumber(null);
            r.setFriend(friendIds.contains(show.getUserId()));
            result.add(r);
        }

        return result;
    }

    // სპოილერ-ფილტრი: ჩანს თუ ეს რივიუ არჩეული სეზონი/ეპიზოდის ჩათვლით ან უკან არის
    private boolean passesSpoilerFilter(int reviewSeason, int reviewEpisode,
                                        Integer uptoSeason, Integer uptoEpisode) {
        // ფილტრი გამორთულია (სერია არ არჩეულა) → ყველა ჩანს
        if (uptoSeason == null || uptoEpisode == null) {
            return true;
        }
        // წინა სეზონი → ყოველთვის ჩანს
        if (reviewSeason < uptoSeason) return true;
        // მომავალი სეზონი → არ ჩანს
        if (reviewSeason > uptoSeason) return false;
        // იგივე სეზონი → მხოლოდ მიმდინარე ეპიზოდის ჩათვლით და უკან
        return reviewEpisode <= uptoEpisode;
    }

    private String resolveUsername(Long userId, Map<Long, String> cache) {
        if (userId == null) return "unknown";
        return cache.computeIfAbsent(userId, id ->
                userRepository.findById(id).map(User::getUsername).orElse("unknown"));
    }
}