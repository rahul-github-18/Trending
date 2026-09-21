package com.thread.Igniter.testing.workflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TIMESTAMP = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String ALICE_USERNAME = "al_" + TIMESTAMP;
    private static final String BOB_USERNAME = "bb_" + TIMESTAMP;
    private static final String PASSWORD = "password123";

    private static Long aliceId;
    private static Long bobId;
    private static String aliceToken;
    private static String bobToken;
    private static Long threadId;
    private static Long commentId;
    private static Long followNotificationId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Health Check -> GET /health")
    void step01_healthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Register Alice and Bob -> POST /api/users")
    void step02_registerUsers() throws Exception {
        // Register Alice
        String aliceJson = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"%s\",\"bio\":\"Alice bio\"}",
                ALICE_USERNAME, ALICE_USERNAME, PASSWORD
        );
        MvcResult aliceRes = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aliceJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(ALICE_USERNAME))
                .andReturn();
        aliceId = ((Number) JsonPath.read(aliceRes.getResponse().getContentAsString(), "$.id")).longValue();

        // Register Bob
        String bobJson = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"%s\",\"bio\":\"Bob bio\"}",
                BOB_USERNAME, BOB_USERNAME, PASSWORD
        );
        MvcResult bobRes = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bobJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(BOB_USERNAME))
                .andReturn();
        bobId = ((Number) JsonPath.read(bobRes.getResponse().getContentAsString(), "$.id")).longValue();

        assertNotNull(aliceId);
        assertNotNull(bobId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Authenticate Alice and Bob -> POST /api/auth/login")
    void step03_loginUsers() throws Exception {
        // Alice Login
        String aliceLogin = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", ALICE_USERNAME, PASSWORD);
        MvcResult aliceRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aliceLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        aliceToken = JsonPath.read(aliceRes.getResponse().getContentAsString(), "$.token");

        // Bob Login
        String bobLogin = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", BOB_USERNAME, PASSWORD);
        MvcResult bobRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bobLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        bobToken = JsonPath.read(bobRes.getResponse().getContentAsString(), "$.token");

        assertNotNull(aliceToken);
        assertNotNull(bobToken);
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Get Current User Profile -> GET /api/users/me")
    void step04_getCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ALICE_USERNAME))
                .andExpect(jsonPath("$.bio").value("Alice bio"));
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Update User Profile -> PUT /api/users/me")
    void step05_updateUserProfile() throws Exception {
        String updateJson = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"bio\":\"Alice updated bio!\"}",
                ALICE_USERNAME, ALICE_USERNAME
        );
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Alice updated bio!"));
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Upload Profile Picture -> POST /api/users/profile-picture")
    void step06_uploadProfilePicture() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "alice-avatar.jpg",
                "image/jpeg",
                "mock-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/users/profile-picture")
                        .file(avatar)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").isNotEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Search Users -> GET /api/users/search?text=")
    void step07_searchUsers() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("text", BOB_USERNAME)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value(BOB_USERNAME));
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Follow User -> POST /api/users/{id}/follow")
    void step08_followUser() throws Exception {
        mockMvc.perform(post("/api/users/" + bobId + "/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Followed Successfully"));
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Verify Follow Status & Counts -> GET /following/status, /count")
    void step09_verifyFollowStatusAndCounts() throws Exception {
        // Alice is following Bob
        mockMvc.perform(get("/api/users/" + bobId + "/following/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Bob has 1 follower
        mockMvc.perform(get("/api/users/" + bobId + "/followers/count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Alice is following 1 user
        mockMvc.perform(get("/api/users/" + aliceId + "/following/count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Check Bob's followers list
        mockMvc.perform(get("/api/users/" + bobId + "/followers")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(ALICE_USERNAME));

        // Check Alice's following list
        mockMvc.perform(get("/api/users/" + aliceId + "/following")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(BOB_USERNAME));
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Bob receives follow notification -> GET /api/notifications")
    void step10_bobReceivesNotification() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].senderUsername").value(ALICE_USERNAME))
                .andReturn();

        followNotificationId = ((Number) JsonPath.read(res.getResponse().getContentAsString(), "$[0].id")).longValue();

        // Check unread count
        mockMvc.perform(get("/api/notifications/unread/count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Mark notification as read
        mockMvc.perform(put("/api/notifications/" + followNotificationId + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Unread count should now be 0
        mockMvc.perform(get("/api/notifications/unread/count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @Order(11)
    @DisplayName("Step 11: Alice creates a thread -> POST /api/threads/create")
    void step11_aliceCreatesThread() throws Exception {
        String threadJson = "{\"content\":\"Hello Igniter community! This is Alice's first post.\"}";

        MvcResult res = mockMvc.perform(post("/api/threads/create")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(threadJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(ALICE_USERNAME))
                .andReturn();

        threadId = ((Number) JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        assertNotNull(threadId);
    }

    @Test
    @Order(12)
    @DisplayName("Step 12: Feed and Get Thread by ID -> GET /api/threads, GET /api/threads/{id}")
    void step12_getThreadsAndById() throws Exception {
        // Feed
        mockMvc.perform(get("/api/threads")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // By ID
        mockMvc.perform(get("/api/threads/" + threadId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(threadId))
                .andExpect(jsonPath("$.username").value(ALICE_USERNAME));
    }

    @Test
    @Order(13)
    @DisplayName("Step 13: Alice updates her thread -> PUT /api/threads/{id}")
    void step13_updateThread() throws Exception {
        String updateJson = "{\"content\":\"Updated thread content by Alice.\"}";

        mockMvc.perform(put("/api/threads/" + threadId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated thread content by Alice."));
    }

    @Test
    @Order(14)
    @DisplayName("Step 14: Bob likes Alice's thread -> POST /api/threads/{id}/like")
    void step14_bobLikesThread() throws Exception {
        mockMvc.perform(post("/api/threads/" + threadId + "/like")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        // Alice receives like notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderUsername").value(BOB_USERNAME))
                .andExpect(jsonPath("$[0].type").value("LIKE"));
    }

    @Test
    @Order(15)
    @DisplayName("Step 15: Bob comments on Alice's thread -> POST /api/threads/{id}/comments")
    void step15_bobCommentsOnThread() throws Exception {
        String commentJson = "{\"content\":\"Awesome post Alice!\"}";

        MvcResult res = mockMvc.perform(post("/api/threads/" + threadId + "/comments")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("Awesome post Alice!"))
                .andExpect(jsonPath("$.username").value(BOB_USERNAME))
                .andReturn();

        commentId = ((Number) JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();

        // Get comments list
        mockMvc.perform(get("/api/threads/" + threadId + "/comments")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Awesome post Alice!"));
    }

    @Test
    @Order(16)
    @DisplayName("Step 16: Bob updates his comment -> PUT /api/threads/comments/{id}")
    void step16_updateComment() throws Exception {
        String updateJson = "{\"content\":\"Awesome post Alice! (Edited)\"}";

        mockMvc.perform(put("/api/threads/comments/" + commentId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Awesome post Alice! (Edited)"));
    }

    @Test
    @Order(17)
    @DisplayName("Step 17: Bob unlikes the thread -> DELETE /api/threads/{id}/like")
    void step17_bobUnlikesThread() throws Exception {
        mockMvc.perform(delete("/api/threads/" + threadId + "/like")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    @Order(18)
    @DisplayName("Step 18: Bob deletes his comment -> DELETE /api/threads/comments/{id}")
    void step18_deleteComment() throws Exception {
        mockMvc.perform(delete("/api/threads/comments/" + commentId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment Deleted"));
    }

    @Test
    @Order(19)
    @DisplayName("Step 19: Alice deletes her thread -> DELETE /api/threads/{id}")
    void step19_deleteThread() throws Exception {
        mockMvc.perform(delete("/api/threads/" + threadId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Thread is successfully deleted"));
    }

    @Test
    @Order(20)
    @DisplayName("Step 20: Alice unfollows Bob -> POST /api/users/{id}/unfollow")
    void step20_unfollowUser() throws Exception {
        mockMvc.perform(post("/api/users/" + bobId + "/unfollow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Unfollowed Successfully"));

        // Status should be false
        mockMvc.perform(get("/api/users/" + bobId + "/following/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Order(21)
    @DisplayName("Step 21: Alice logs out and token is revoked -> POST /api/auth/logout")
    void step21_logoutAndRevocation() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged Out SuccessFully"));

        // Alice token should now be rejected as unauthorized
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isUnauthorized());
    }
}
