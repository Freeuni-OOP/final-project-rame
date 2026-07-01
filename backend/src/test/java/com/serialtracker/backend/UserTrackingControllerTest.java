package com.serialtracker.backend;

import com.serialtracker.backend.controller.UserTrackingController;
import com.serialtracker.backend.entity.Recommendation;
import com.serialtracker.backend.repository.RecommendationRepository;
import com.serialtracker.backend.repository.UserEpisodeStatusRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.repository.UserShowStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTrackingControllerTest {

    // 1. დეკლარაციას ვუკეთებთ დასამოკებელ რეპოზიტორებს
    private UserShowStatusRepository statusRepository;
    private UserEpisodeStatusRepository episodeRepository;
    private UserRepository userRepository;
    private RecommendationRepository recommendationRepository;
    private com.serialtracker.backend.repository.ActivityRepository activityRepository;
    // უშუალოდ გასატესტი კონტროლერი
    private UserTrackingController userTrackingController;

    @BeforeEach
    void setUp() {
        // 2. ვქმნით უბრალო Mock-ებს Spring-ის გარეშე
        statusRepository = mock(UserShowStatusRepository.class);
        episodeRepository = mock(UserEpisodeStatusRepository.class);
        userRepository = mock(UserRepository.class);
        recommendationRepository = mock(RecommendationRepository.class);
        activityRepository = mock(com.serialtracker.backend.repository.ActivityRepository.class);
        // 3. კონტროლერის ინიციალიზაცია ჩვეულებრივი კონსტრუქტორით
        userTrackingController = new UserTrackingController(
                statusRepository, episodeRepository, userRepository, recommendationRepository, activityRepository
        );
    }

    /**
     * 🟢 ტესტი 1: წარმატებული რეკომენდაციის გაგზავნა (200 OK)
     */
    @Test
    void recommendShow_Success() {
        // მოცემულობა (Given)
        when(recommendationRepository.existsBySenderUsernameAndTargetUsernameAndShowId("user123", "friend123", 150))
                .thenReturn(false);

        // მოქმედება (When)
        ResponseEntity<?> response = userTrackingController.recommendShow(
                "user123", "friend123", 150, "Dr. STONE", "Check this out!"
        );

        // შემოწმება (Then)
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recommendation sent successfully!", response.getBody());

        // ვრწმუნდებით, რომ ბაზაში შენახვის მეთოდი მართლაც გამოიძახა
        verify(recommendationRepository, times(1)).save(any(Recommendation.class));
    }

    /**
     * 🛑 ტესტი 2: დუბლიკატი რეკომენდაციის ბლოკირება (400 Bad Request)
     */
    @Test
    void recommendShow_AlreadyExists_ReturnsBadRequest() {
        // მოცემულობა (Given): სიმულაცია, რომ ეს რეკომენდაცია უკვე არსებობს
        when(recommendationRepository.existsBySenderUsernameAndTargetUsernameAndShowId("user123", "friend123", 150))
                .thenReturn(true);

        // მოქმედება (When)
        ResponseEntity<?> response = userTrackingController.recommendShow(
                "user123", "friend123", 150, "Dr. STONE", "Check this out!"
        );

        // შემოწმება (Then)
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("You have already recommended this show to this friend!", response.getBody());

        // ვრწმუნდებით, რომ ახალი ჩანაწერი ბაზაში არ შენახულა
        verify(recommendationRepository, never()).save(any(Recommendation.class));
    }

    /**
     * 🟢 ტესტი 3: გაგზავნილი რეკომენდაციების სიის წამოღება
     */
    @Test
    void getSentRecommendations_Success() {
        // მოცემულობა (Given)
        Recommendation mockRec = new Recommendation("user123", "friend123", 150, "Dr. STONE", "Cool!");
        when(recommendationRepository.findBySenderUsername("user123"))
                .thenReturn(Collections.singletonList(mockRec));

        // მოქმედება (When)
        ResponseEntity<?> response = userTrackingController.getSentRecommendations("user123");

        // შემოწმება (Then)
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Recommendation> resultList = (List<Recommendation>) response.getBody();
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        assertEquals("friend123", resultList.get(0).getTargetUsername());
    }
}