package com.serialtracker.backend.controller;

import com.serialtracker.backend.service.TMDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = "http://localhost:5173")
public class ShowController {

    @Autowired
    private TMDBService tmdbService;

    @GetMapping(value = "/popular", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPopular(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getPopularShows(page));
    }

    @GetMapping(value = "/trending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTrending() {
        return ResponseEntity.ok(tmdbService.getTrendingShows());
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.searchShows(query, page));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getShowDetails(@PathVariable int id) {
        return ResponseEntity.ok(tmdbService.getShowDetails(id));
    }

    @GetMapping(value = "/{id}/credits", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCredits(@PathVariable int id) {
        return ResponseEntity.ok(tmdbService.getShowCredits(id));
    }

    @GetMapping(value = "/{id}/season/{seasonNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSeason(
            @PathVariable int id,
            @PathVariable int seasonNumber) {
        return ResponseEntity.ok(tmdbService.getSeasonDetails(id, seasonNumber));
    }

    @GetMapping(value = "/{id}/season/{seasonNumber}/episode/{episodeNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getEpisode(
            @PathVariable int id,
            @PathVariable int seasonNumber,
            @PathVariable int episodeNumber) {
        return ResponseEntity.ok(tmdbService.getEpisodeDetails(id, seasonNumber, episodeNumber));
    }

    @GetMapping(value = "/genre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getByGenre(
            @RequestParam String genreId,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getShowsByGenre(genreId, page));
    }

    
}