package com.serialtracker.backend.service;

import com.serialtracker.backend.dto.ReviewResponse;
import com.serialtracker.backend.entity.Friendship;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.entity.UserEpisodeStatus;
import com.serialtracker.backend.entity.UserShowStatus;
import com.serialtracker.backend.entity.ReviewLike;
import com.serialtracker.backend.repository.FriendshipRepository;
import com.serialtracker.backend.repository.ReviewLikeRepository;
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

    public static final String TYPE_EPISODE = "EPISODE";
    public static final String TYPE_SHOW = "SHOW";

    private final UserRepository userRepository;
    private final UserShowStatusRepository showStatusRepository;
    private final UserEpisodeStatusRepository episodeStatusRepository;
    private final FriendshipRepository friendshipRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    public ReviewService(UserRepository userRepository,
                         UserShowStatusRepository showStatusRepository,
                         UserEpisodeStatusRepository episodeStatusRepository,
                         FriendshipRepository friendshipRepository,
                         ReviewLikeRepository reviewLikeRepository) {
        this.userRepository = userRepository;
        this.showStatusRepository = showStatusRepository;
        this.episodeStatusRepository = episodeStatusRepository;
        this.friendshipRepository = friendshipRepository;
        this.reviewLikeRepository = reviewLikeRepository;
    }

    /**
     * რევიუს ლაიქის ჩართვა/გამორთვა (toggle).
     * @return [liked, likeCount] — ახალი მდგომარეობა.
     */
    public Map<String, Object> toggleLike(String username, String reviewType, Long reviewId) {
        Long likerId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();

        boolean liked;
        var existing = reviewLikeRepository
                .findByLikerUserIdAndReviewTypeAndReviewId(likerId, reviewType, reviewId);
        if (existing.isPresent()) {
            reviewLikeRepository.delete(existing.get());
            liked = false;
        } else {
            reviewLikeRepository.save(new ReviewLike(likerId, reviewType, reviewId));
            liked = true;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("liked", liked);
        res.put("likeCount", reviewLikeRepository.countByReviewTypeAndReviewId(reviewType, reviewId));
        return res;
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

        // 1b) მიმდინარე მნახველის ლაიქები (TYPE:id კომპლექტად, სწრაფი lookup-ისთვის)
        Set<String> myLikedKeys = new HashSet<>();
        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(me -> {
                for (ReviewLike rl : reviewLikeRepository.findByLikerUserId(me.getId())) {
                    myLikedKeys.add(rl.getReviewType() + ":" + rl.getReviewId());
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
            applyLikes(r, TYPE_EPISODE, ep.getId(), myLikedKeys);
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
            applyLikes(r, TYPE_SHOW, show.getId(), myLikedKeys);
            result.add(r);
        }

        return result;
    }

    // რევიუზე ლაიქების რაოდენობა + reviewId/type + მნახველმა დაალაიქა თუ არა
    private void applyLikes(ReviewResponse r, String type, Long reviewId, Set<String> myLikedKeys) {
        r.setReviewId(reviewId);
        r.setReviewType(type);
        r.setLikeCount(reviewLikeRepository.countByReviewTypeAndReviewId(type, reviewId));
        r.setLikedByMe(myLikedKeys.contains(type + ":" + reviewId));
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