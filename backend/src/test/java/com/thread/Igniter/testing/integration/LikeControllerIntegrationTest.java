package com.thread.Igniter.testing.integration;

import com.thread.Igniter.security.service.JwtService;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LikeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String LIKER_USER = "like_usr";
    private String likerToken;
    private Thread testThread;

    @BeforeEach
    void setUp() {
        User user;
        if (!userRepository.existsByUsername(LIKER_USER)) {
            user = new User();
            user.setUsername(LIKER_USER);
            user.setEmail("like_usr@test.com");
            user.setPassword(passwordEncoder.encode("pass123"));
            user = userRepository.save(user);
        } else {
            user = userRepository.findByUsername(LIKER_USER).orElseThrow();
        }

        likerToken = jwtService.generateToken(LIKER_USER);

        Thread thread = new Thread();
        thread.setContent("Thread for likes");
        thread.setUser(user);
        thread.setLikeCount(0);
        testThread = threadRepository.save(thread);
    }

    @Test
    @DisplayName("POST /api/threads/{id}/like likes thread and DELETE /api/threads/{id}/like unlikes")
    void testLikeAndUnlikeThread() throws Exception {
        // 1. Like the thread
        mockMvc.perform(post("/api/threads/" + testThread.getId() + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        // 2. Unlike the thread
        mockMvc.perform(delete("/api/threads/" + testThread.getId() + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }
}
