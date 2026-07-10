package com.serialtracker.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.MovieListLikeRepository;
import com.serialtracker.backend.repository.MovieListRepository;
import com.serialtracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class MovieListControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private MovieListLikeRepository movieListLikeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String OWNER = "list_ctrl_owner";
    private final String LIKER = "list_ctrl_liker";

    private Long listId;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        for (String username : new String[]{OWNER, LIKER}) {
            User u = new User();
            u.setUsername(username);
            u.setPassword("password123");
            u.setEmail(username + "@test.com");
            userRepository.save(u);
        }

        // ახალი სია
        MvcResult result = mockMvc.perform(post("/api/lists")
                        .param("actingUsername", OWNER)
                        .param("name", "My Test List")
                        .param("isPublic", "true"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        listId = body.get("id").asLong();
    }

    // 1. GET /api/lists/{id} — username-ის გარეშე, likedByMe=false
    @Test
    void getList_noUsername_likedByMeFalse() throws Exception {
        mockMvc.perform(get("/api/lists/" + listId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    // 2. GET /api/lists/{id}?username=X — username-ით, ჯერ კიდევ false (არ დაულაიქია)
    @Test
    void getList_withUsername_likedByMeFalse_beforeLike() throws Exception {
        mockMvc.perform(get("/api/lists/" + listId)
                        .param("username", LIKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    // 3. POST like → GET-ში likedByMe=true, likeCount=1
    @Test
    void toggleLike_then_getList_showsLikedByMe() throws Exception {
        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lists/" + listId)
                        .param("username", LIKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    // 4. like → like (toggle off) → likeCount=0, likedByMe=false
    @Test
    void toggleLike_twice_unlikesCorrectly() throws Exception {
        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lists/" + listId)
                        .param("username", LIKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    // 5. GET /api/lists/liked?username=X — liked სიაში ჩანს ლაიქის შემდეგ
    @Test
    void getRecentlyLiked_returnsLikedList() throws Exception {
        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lists/liked")
                        .param("username", LIKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(listId.intValue())));
    }

    // 6. GET /api/lists/liked — unlike-ის შემდეგ სიაში აღარ ჩანს
    @Test
    void getRecentlyLiked_afterUnlike_listIsEmpty() throws Exception {
        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", LIKER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lists/liked")
                        .param("username", LIKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(listId.intValue()))));
    }

    // 7. GET /api/lists/public/all — public სია ჩანს
    @Test
    void getPublicLists_includesPublicList() throws Exception {
        mockMvc.perform(get("/api/lists/public/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(listId.intValue())));
    }

    // 8. owner თვითონ ალაიქებს საკუთარ სიას — likedByMe=true
    @Test
    void getList_ownerLikesOwnList_likedByMeTrue() throws Exception {
        mockMvc.perform(post("/api/lists/" + listId + "/like")
                        .param("username", OWNER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lists/" + listId)
                        .param("username", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(true));
    }
}
