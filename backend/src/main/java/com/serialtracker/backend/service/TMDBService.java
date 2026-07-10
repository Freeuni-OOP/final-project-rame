package com.serialtracker.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TMDBService {

    @Value("${tmdb.api.key:a1de4b53c576eb9c6c54e242b324e8b1}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getPopularShows(int page) {
        String url = baseUrl + "/tv/popular?api_key=" + apiKey
                + "&language=en-US&page=" + page;
        return restTemplate.getForObject(url, String.class);
    }

    public String searchShows(String query, int page) {
        String encodedQuery = query.replace(" ", "+");
        String url = baseUrl + "/search/tv?api_key=" + apiKey
                + "&query=" + encodedQuery
                + "&language=en-US&page=" + page;
        return restTemplate.getForObject(url, String.class);
    }

    public String getShowDetails(int showId) {
        String url = baseUrl + "/tv/" + showId
                + "?api_key=" + apiKey + "&language=en-US";
        return restTemplate.getForObject(url, String.class);
    }

    public String getSeasonDetails(int showId, int seasonNumber) {
        String url = baseUrl + "/tv/" + showId
                + "/season/" + seasonNumber
                + "?api_key=" + apiKey + "&language=en-US";
        return restTemplate.getForObject(url, String.class);
    }

    public String getEpisodeDetails(int showId, int seasonNumber, int episodeNumber) {
        String url = baseUrl + "/tv/" + showId
                + "/season/" + seasonNumber
                + "/episode/" + episodeNumber
                + "?api_key=" + apiKey + "&language=en-US";
        return restTemplate.getForObject(url, String.class);
    }

    public String getShowCredits(int showId) {
        String url = baseUrl + "/tv/" + showId
                + "/credits?api_key=" + apiKey + "&language=en-US";
        return restTemplate.getForObject(url, String.class);
    }

    public String getTrendingShows() {
        String url = baseUrl + "/trending/tv/week?api_key=" + apiKey;
        return restTemplate.getForObject(url, String.class);
    }

    public String getShowsByGenre(String genreId, int page) {
        String url = "https://api.themoviedb.org/3/discover/tv?api_key=" + apiKey + "&with_genres=" + genreId + "&page=" + page;
        return restTemplate.getForObject(url, String.class);
    }

    /** შოუს სახელი და პოსტერი — ლენტისთვის, სადაც ეს ველები ბაზაში არ გვაქვს. */
    public record ShowSummary(String name, String posterPath) {}

    private final Map<Integer, ShowSummary> summaryCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * TMDB-ის მოთხოვნა ერთ შოუზე, ქეშირებული. ჩავარდნისას აბრუნებს ცარიელ summary-ს,
     * რომ ერთმა წაშლილმა შოუმ მთელი ლენტა არ ჩააგდოს.
     */
    public ShowSummary getShowSummary(int showId) {
        return summaryCache.computeIfAbsent(showId, id -> {
            try {
                JsonNode node = objectMapper.readTree(getShowDetails(id));
                String name = node.path("name").asText(null);
                String poster = node.path("poster_path").asText(null);
                return new ShowSummary(name, poster);
            } catch (Exception e) {
                return new ShowSummary(null, null);
            }
        });
    }
}