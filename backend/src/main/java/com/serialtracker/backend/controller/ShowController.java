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

    // ახალი ენდპოინტი სერიალის მეგობრისთვის რეკომენდაციისთვის
    @PostMapping("/{id}/recommend")
    public ResponseEntity<?> recommendShow(
            @PathVariable int id,
            @RequestParam String senderUsername,
            @RequestParam String targetUsername) {
        try {
            // აქ დროებით უბრალოდ დავბეჭდოთ კონსოლში იმიტაციისთვის
            System.out.println("RECOMMENDATION: User '" + senderUsername +
                    "' recommended Show ID: " + id + " to User '" + targetUsername + "'");

            // მომავალში აქ ჩაჯდება სერვისის გამოძახება, მაგ: recommendationService.send(...)

            return ResponseEntity.ok("Show recommended successfully to " + targetUsername);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to recommend show: " + e.getMessage());
        }
    }

    @GetMapping(value = "/genre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getByGenre(
            @RequestParam String genreId,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getShowsByGenre(genreId, page));
    }


}