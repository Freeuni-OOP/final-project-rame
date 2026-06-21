package com.serialtracker.backend;


import com.serialtracker.backend.service.TMDBService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

//tests for apiconection
@SpringBootTest
public class TMDBServiceTest {

    @Autowired
    private TMDBService tmdbService;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(tmdbService, "restTemplate");
        assert restTemplate != null;
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testGetPopularShows_Success() {
        String mockResponse = "{\"page\":1,\"results\":[{\"id\":123,\"name\":\"Test Popular Show\"}]}";
        String expectedUrl = "https://api.themoviedb.org/3/tv/popular?api_key=a1de4b53c576eb9c6c54e242b324e8b1&language=en-US&page=1";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getPopularShows(1);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testSearchShows_Success() {
        String mockResponse = "{\"results\":[{\"id\":456,\"name\":\"Breaking Bad\"}]}";
        String expectedUrl = "https://api.themoviedb.org/3/search/tv?api_key=a1de4b53c576eb9c6c54e242b324e8b1&query=Breaking+Bad&language=en-US&page=1";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.searchShows("Breaking Bad", 1);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testGetShowDetails_Success() {
        String mockResponse = "{\"id\":999,\"name\":\"Game of Thrones\"}";
        String expectedUrl = "https://api.themoviedb.org/3/tv/999?api_key=a1de4b53c576eb9c6c54e242b324e8b1&language=en-US";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getShowDetails(999);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testGetSeasonDetails_Success() {
        String mockResponse = "{\"id\":111,\"season_number\":1,\"name\":\"Season 1\"}";
        String expectedUrl = "https://api.themoviedb.org/3/tv/999/season/1?api_key=a1de4b53c576eb9c6c54e242b324e8b1&language=en-US";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getSeasonDetails(999, 1);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testGetEpisodeDetails_Success() {
        String mockResponse = "{\"id\":222,\"episode_number\":5,\"name\":\"Winter is Coming\"}";
        String expectedUrl = "https://api.themoviedb.org/3/tv/999/season/1/episode/5?api_key=a1de4b53c576eb9c6c54e242b324e8b1&language=en-US";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getEpisodeDetails(999, 1, 5);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testGetShowCredits_Success() {
        String mockResponse = "{\"id\":999,\"cast\":[],\"crew\":[]}";
        String expectedUrl = "https://api.themoviedb.org/3/tv/999/credits?api_key=a1de4b53c576eb9c6c54e242b324e8b1&language=en-US";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getShowCredits(999);
        assertEquals(mockResponse, result);
        mockServer.verify();
    }

    @Test
    public void testGetTrendingShows_Success() {
        String mockResponse = "{\"results\":[{\"id\":777,\"name\":\"Trending Show\"}]}";
        String expectedUrl = "https://api.themoviedb.org/3/trending/tv/week?api_key=a1de4b53c576eb9c6c54e242b324e8b1";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String result = tmdbService.getTrendingShows();
        assertEquals(mockResponse, result);
        mockServer.verify();
    }
}