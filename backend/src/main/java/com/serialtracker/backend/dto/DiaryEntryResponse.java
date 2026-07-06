package com.serialtracker.backend.dto;

import java.time.LocalDate;

/**
 * ერთი Diary-ჩანაწერი: რა ნახა იუზერმა და როდის.
 * title/poster/released წელი აქ არ არის — front-end-ი ამათ showId-ით
 * TMDB-დან იღებს (როგორც watchlist-ში), რომ backend მარტივი დარჩეს.
 */
public class DiaryEntryResponse {

    private int showId;
    private Integer seasonNumber;   // null თუ whole-show ჩანაწერია
    private Integer episodeNumber;  // null თუ whole-show ჩანაწერია
    private LocalDate watchDate;
    private Integer rating;
    private boolean liked;
    private boolean rewatch;
    private String review;
    private boolean wholeShow;

    // Review-likes
    private Long reviewId;       // წყაროს row id (episode ან show სტატუსში)
    private String reviewType;   // "EPISODE" | "SHOW"
    private long likeCount;
    private boolean likedByMe;   // მიმდინარე მნახველმა დაალაიქა თუ არა

    public DiaryEntryResponse() {}

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }

    public LocalDate getWatchDate() { return watchDate; }
    public void setWatchDate(LocalDate watchDate) { this.watchDate = watchDate; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public boolean isWholeShow() { return wholeShow; }
    public void setWholeShow(boolean wholeShow) { this.wholeShow = wholeShow; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
}
