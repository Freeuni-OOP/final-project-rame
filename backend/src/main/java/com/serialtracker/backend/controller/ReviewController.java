package com.serialtracker.backend.controller;

import com.serialtracker.backend.dto.ReviewResponse;
import com.serialtracker.backend.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // GET /api/reviews/{showId}?username=Niakoo&season=1&episode=3
    // season/episode არასავალდებულოა — თუ არ არის, ფილტრი გამორთულია (ყველა რივიუ ჩანს)
    @GetMapping("/{showId}")
    public List<ReviewResponse> getReviews(
            @PathVariable int showId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) Integer episode) {

        return reviewService.getReviewsForShow(showId, username, season, episode);
    }
}