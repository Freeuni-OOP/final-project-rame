package com.serialtracker.backend.dto;

import java.time.LocalDate;

/**
 * მეგობრების ლენტის ერთი პოსტი — ვინ, რა შოუ და რა დაწერა.
 */
public class FeedItemResponse {

    private String username;
    private String profilePicture;   // Base64, null თუ ავატარი არ აქვს

    private int showId;
    private String showName;
    private String posterPath;       // TMDB-ის ფარდობითი გზა, მაგ. "/abc.jpg"

    private Integer rating;
    private String review;           // null თუ მხოლოდ შეფასებაა
    private boolean liked;           // ავტორის გულაკი
    private boolean rewatch;
    private LocalDate watchDate;

    private Integer seasonNumber;    // null თუ Whole Show რივიუა
    private Integer episodeNumber;   // null თუ Whole Show რივიუა

    private Long reviewId;
    private String reviewType;       // "EPISODE" | "SHOW"
    private long likeCount;
    private boolean likedByMe;

    public FeedItemResponse() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public String getShowName() { return showName; }
    public void setShowName(String showName) { this.showName = showName; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public LocalDate getWatchDate() { return watchDate; }
    public void setWatchDate(LocalDate watchDate) { this.watchDate = watchDate; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
}
