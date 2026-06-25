package com.serialtracker.backend.dto;

public class LogRequest {
    private String username;
    private int showId;
    private int rating;
    private String review;
    private Boolean liked;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private Boolean wholeShow;
    private Boolean rewatch;
    private String watchDate;

    public Boolean getRewatch() { return rewatch; }
    public void setRewatch(Boolean rewatch) { this.rewatch = rewatch; }

    public String getWatchDate() { return watchDate; }
    public void setWatchDate(String watchDate) { this.watchDate = watchDate; }

    public Boolean getWholeShow() { return wholeShow; }
    public void setWholeShow(Boolean wholeShow) { this.wholeShow = wholeShow; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
}