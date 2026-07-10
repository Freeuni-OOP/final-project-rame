package com.serialtracker.backend.service;

import com.serialtracker.backend.dto.FeedItemResponse;
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
import java.util.Comparator;
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
    private final TMDBService tmdbService;

    public ReviewService(UserRepository userRepository,
                         UserShowStatusRepository showStatusRepository,
                         UserEpisodeStatusRepository episodeStatusRepository,
                         FriendshipRepository friendshipRepository,
                         ReviewLikeRepository reviewLikeRepository,
                         TMDBService tmdbService) {
        this.userRepository = userRepository;
        this.showStatusRepository = showStatusRepository;
        this.episodeStatusRepository = episodeStatusRepository;
        this.friendshipRepository = friendshipRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.tmdbService = tmdbService;
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
     * მთავარი გვერდის ლენტა: მხოლოდ მეგობრების ჩანაწერები, სადაც რივიუ ან რეიტინგია.
     * ახლიდან ძველისკენ; ჯერ watchDate, თარიღის უქონლობისას — ჩანაწერის id.
     */
    public List<FeedItemResponse> getFriendsFeed(String currentUsername, int limit) {
        User me = userRepository.findByUsername(currentUsername).orElse(null);
        if (me == null) return List.of();

        Set<Long> friendIds = new HashSet<>();
        for (Friendship f : friendshipRepository.findAcceptedFriendshipsOf(me)) {
            Long otherId = f.getRequester().getId().equals(me.getId())
                    ? f.getRecipient().getId()
                    : f.getRequester().getId();
            friendIds.add(otherId);
        }
        if (friendIds.isEmpty()) return List.of();

        Set<String> myLikedKeys = new HashSet<>();
        for (ReviewLike rl : reviewLikeRepository.findByLikerUserId(me.getId())) {
            myLikedKeys.add(rl.getReviewType() + ":" + rl.getReviewId());
        }

        Map<Long, User> userCache = new HashMap<>();
        List<FeedItemResponse> feed = new ArrayList<>();

        for (UserEpisodeStatus ep : episodeStatusRepository.findFeedEntriesByUserIds(friendIds)) {
            if (isBlank(ep.getReview()) && ep.getRating() == null) continue;
            FeedItemResponse item = newFeedItem(ep.getUserId(), ep.getShowId(), userCache);
            item.setRating(ep.getRating());
            item.setReview(isBlank(ep.getReview()) ? null : ep.getReview());
            item.setLiked(ep.isLiked());
            item.setRewatch(ep.isRewatch());
            item.setWatchDate(ep.getWatchDate());
            item.setSeasonNumber(ep.getSeasonNumber());
            item.setEpisodeNumber(ep.getEpisodeNumber());
            applyFeedLikes(item, TYPE_EPISODE, ep.getId(), myLikedKeys);
            feed.add(item);
        }

        for (UserShowStatus show : showStatusRepository.findFeedEntriesByUserIds(friendIds)) {
            if (isBlank(show.getReview()) && show.getRating() == null) continue;
            FeedItemResponse item = newFeedItem(show.getUserId(), show.getShowId(), userCache);
            item.setRating(show.getRating());
            item.setReview(isBlank(show.getReview()) ? null : show.getReview());
            item.setLiked(show.isFavorite());
            item.setRewatch(show.isRewatch());
            item.setWatchDate(show.getWatchDate());
            applyFeedLikes(item, TYPE_SHOW, show.getId(), myLikedKeys);
            feed.add(item);
        }

        feed.sort(Comparator
                .comparing(FeedItemResponse::getWatchDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FeedItemResponse::getReviewId, Comparator.reverseOrder()));

        return hydrateTop(feed, limit);
    }

    /**
     * საჯარო რივიუების ლენტა: ყველა იუზერის ჩანაწერი, სადაც რივიუ ან რეიტინგია.
     * sort="popular" — ლაიქების მიხედვით; ნებისმიერი სხვა — უახლესი ჯერ (ჩანაწერის id-ით).
     */
    public List<FeedItemResponse> getPublicFeed(String currentUsername, String sort, int limit) {
        Set<String> myLikedKeys = new HashSet<>();
        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(me -> {
                for (ReviewLike rl : reviewLikeRepository.findByLikerUserId(me.getId())) {
                    myLikedKeys.add(rl.getReviewType() + ":" + rl.getReviewId());
                }
            });
        }

        Map<Long, User> userCache = new HashMap<>();
        List<FeedItemResponse> feed = new ArrayList<>();

        for (UserEpisodeStatus ep : episodeStatusRepository.findAllReviewed()) {
            if (isBlank(ep.getReview()) && ep.getRating() == null) continue;
            FeedItemResponse item = newFeedItem(ep.getUserId(), ep.getShowId(), userCache);
            item.setRating(ep.getRating());
            item.setReview(isBlank(ep.getReview()) ? null : ep.getReview());
            item.setLiked(ep.isLiked());
            item.setRewatch(ep.isRewatch());
            item.setWatchDate(ep.getWatchDate());
            item.setSeasonNumber(ep.getSeasonNumber());
            item.setEpisodeNumber(ep.getEpisodeNumber());
            applyFeedLikes(item, TYPE_EPISODE, ep.getId(), myLikedKeys);
            feed.add(item);
        }

        for (UserShowStatus show : showStatusRepository.findAllReviewed()) {
            if (isBlank(show.getReview()) && show.getRating() == null) continue;
            FeedItemResponse item = newFeedItem(show.getUserId(), show.getShowId(), userCache);
            item.setRating(show.getRating());
            item.setReview(isBlank(show.getReview()) ? null : show.getReview());
            item.setLiked(show.isFavorite());
            item.setRewatch(show.isRewatch());
            item.setWatchDate(show.getWatchDate());
            applyFeedLikes(item, TYPE_SHOW, show.getId(), myLikedKeys);
            feed.add(item);
        }

        Comparator<FeedItemResponse> newest =
                Comparator.comparing(FeedItemResponse::getReviewId, Comparator.reverseOrder());

        if ("popular".equalsIgnoreCase(sort)) {
            feed.sort(Comparator.comparingLong(FeedItemResponse::getLikeCount).reversed()
                    .thenComparing(newest));
        } else {
            feed.sort(newest);
        }

        return hydrateTop(feed, limit);
    }

    /** ჭრის ჩამონათვალს limit-ამდე და მხოლოდ ამ ნაწილს ავსებს TMDB-ის სახელით/პოსტერით. */
    private List<FeedItemResponse> hydrateTop(List<FeedItemResponse> feed, int limit) {
        List<FeedItemResponse> top = feed.size() > limit ? feed.subList(0, limit) : feed;
        for (FeedItemResponse item : top) {
            TMDBService.ShowSummary summary = tmdbService.getShowSummary(item.getShowId());
            item.setShowName(summary.name());
            item.setPosterPath(summary.posterPath());
        }
        return top;
    }

    /** ავტორი + showId. შოუს სახელი/პოსტერი ცალკე, hydrateTop-ში ივსება. */
    private FeedItemResponse newFeedItem(Long authorId, int showId, Map<Long, User> userCache) {
        FeedItemResponse item = new FeedItemResponse();

        User author = userCache.computeIfAbsent(authorId, id -> userRepository.findById(id).orElse(null));
        if (author != null) {
            item.setUsername(author.getUsername());
            if (author.getProfilePicture() != null) {
                item.setProfilePicture(java.util.Base64.getEncoder().encodeToString(author.getProfilePicture()));
            }
        } else {
            item.setUsername("Unknown");
        }

        item.setShowId(showId);
        return item;
    }

    private void applyFeedLikes(FeedItemResponse item, String type, Long reviewId, Set<String> myLikedKeys) {
        item.setReviewId(reviewId);
        item.setReviewType(type);
        item.setLikeCount(reviewLikeRepository.countByReviewTypeAndReviewId(type, reviewId));
        item.setLikedByMe(myLikedKeys.contains(type + ":" + reviewId));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public List<ReviewResponse> getReviewsForShow(int showId, String currentUsername,
                                                  Integer uptoSeason, Integer uptoEpisode) {

        // 1) მიმდინარე იუზერის მეგობრების userId-ების ნაკრები
        Set<Long> friendIds = new HashSet<>();
        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(me -> {
                for (Friendship f : friendshipRepository.findAcceptedFriendshipsOf(me)) {
                    Long otherId = f.getRequester().getId().equals(me.getId())
                            ? f.getRecipient().getId()
                            : f.getRequester().getId();
                    friendIds.add(otherId);
                }
            });
        }

        // 1b) მიმდინარე მნახველის ლაიქები
        Set<String> myLikedKeys = new HashSet<>();
        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(me -> {
                for (ReviewLike rl : reviewLikeRepository.findByLikerUserId(me.getId())) {
                    myLikedKeys.add(rl.getReviewType() + ":" + rl.getReviewId());
                }
            });
        }

        // 2) 🟢 იუზერის ობიექტების ქეში (რომ ბაზა არ გადაიტვირთოს)
        Map<Long, User> userCache = new HashMap<>();
        List<ReviewResponse> result = new ArrayList<>();

        // 3) ეპიზოდების რივიუები
        for (UserEpisodeStatus ep : episodeStatusRepository.findByShowIdAndReviewIsNotNull(showId)) {
            if (!passesSpoilerFilter(ep.getSeasonNumber(), ep.getEpisodeNumber(), uptoSeason, uptoEpisode)) {
                continue;
            }
            ReviewResponse r = new ReviewResponse();

            // 🟢 ვიღებთ ან ვაქეშირებთ იუზერს
            User user = userCache.computeIfAbsent(ep.getUserId(), id -> userRepository.findById(id).orElse(null));
            if (user != null) {
                r.setUsername(user.getUsername());
                if (user.getProfilePicture() != null) {
                    r.setProfilePicture(java.util.Base64.getEncoder().encodeToString(user.getProfilePicture()));
                }
            } else {
                r.setUsername("Unknown");
            }

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

        // 4) Whole Show რივიუები
        for (UserShowStatus show : showStatusRepository.findByShowIdAndReviewIsNotNull(showId)) {
            ReviewResponse r = new ReviewResponse();

            // 🟢 იგივე ქეშირება მთლიანი შოუს რევიუებისთვის
            User user = userCache.computeIfAbsent(show.getUserId(), id -> userRepository.findById(id).orElse(null));
            if (user != null) {
                r.setUsername(user.getUsername());
                if (user.getProfilePicture() != null) {
                    r.setProfilePicture(java.util.Base64.getEncoder().encodeToString(user.getProfilePicture()));
                }
            } else {
                r.setUsername("Unknown");
            }

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