package com.thread.Igniter.testing.integration;

import com.thread.Igniter.follow.repository.FollowRepository;
import com.thread.Igniter.security.service.JwtService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FollowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String FOLLOWER = "flw_er";
    private static final String TARGET = "flw_tgt";
    private String followerToken;
    private User targetUser;
    private User followerUser;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername(FOLLOWER)) {
            followerUser = new User();
            followerUser.setUsername(FOLLOWER);
            followerUser.setEmail("flw_er@test.com");
            followerUser.setPassword(passwordEncoder.encode("pass123"));
            followerUser = userRepository.save(followerUser);
        } else {
            followerUser = userRepository.findByUsername(FOLLOWER).orElseThrow();
        }

        if (!userRepository.existsByUsername(TARGET)) {
            targetUser = new User();
            targetUser.setUsername(TARGET);
            targetUser.setEmail("flw_tgt@test.com");
            targetUser.setPassword(passwordEncoder.encode("pass123"));
            targetUser = userRepository.save(targetUser);
        } else {
            targetUser = userRepository.findByUsername(TARGET).orElseThrow();
        }

        followerToken = jwtService.generateToken(FOLLOWER);

        // Clean any existing follow relationship between these two
        followRepository.findByFollowerIdAndFollowingId(followerUser.getId(), targetUser.getId())
                .ifPresent(followRepository::delete);
    }

    @Test
    @DisplayName("Follow, check status, verify counts, get followers/following, and unfollow")
    void testCompleteFollowWorkflow() throws Exception {
        Long targetId = targetUser.getId();

        // 1. Initially isFollowing should be false
        mockMvc.perform(get("/api/users/" + targetId + "/following/status")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // 2. Follow target user
        mockMvc.perform(post("/api/users/" + targetId + "/follow")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Followed Successfully"));

        // 3. Status should now be true
        mockMvc.perform(get("/api/users/" + targetId + "/following/status")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 4. Followers count for target should be >= 1
        mockMvc.perform(get("/api/users/" + targetId + "/followers/count")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk());

        // 5. Following count for follower should be >= 1
        mockMvc.perform(get("/api/users/" + followerUser.getId() + "/following/count")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk());

        // 6. Get followers list of target
        mockMvc.perform(get("/api/users/" + targetId + "/followers")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 7. Get following list of follower
        mockMvc.perform(get("/api/users/" + followerUser.getId() + "/following")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 8. Unfollow target user
        mockMvc.perform(post("/api/users/" + targetId + "/unfollow")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Unfollowed Successfully"));

        // 9. Status should now be false again
        mockMvc.perform(get("/api/users/" + targetId + "/following/status")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
