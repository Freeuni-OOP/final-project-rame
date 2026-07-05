package com.serialtracker.backend.dto;

public class ReviewResponse {

    private String username;
    private Integer rating;
    private String review;
    private boolean liked;
    private boolean rewatch;
    private Integer seasonNumber;   // null თუ Whole Show რივიუა
    private Integer episodeNumber;  // null თუ Whole Show რივიუა
    private boolean friend;         // true თუ ეს მიმდინარე იუზერის მეგობარია

    public ReviewResponse() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }

    public boolean isFriend() { return friend; }
    public void setFriend(boolean friend) { this.friend = friend; }
}