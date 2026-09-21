package com.thread.Igniter.testing.integration;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ThreadControllerIntegrationTest {

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

    private static final String AUTHOR_USER = "thrd_author";
    private static final String OTHER_USER = "thrd_other";
    private String authorToken;
    private String otherToken;
    private User author;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername(AUTHOR_USER)) {
            User user = new User();
            user.setUsername(AUTHOR_USER);
            user.setEmail("thrd_author@test.com");
            user.setPassword(passwordEncoder.encode("pass123"));
            author = userRepository.save(user);
        } else {
            author = userRepository.findByUsername(AUTHOR_USER).orElseThrow();
        }

        if (!userRepository.existsByUsername(OTHER_USER)) {
            User other = new User();
            other.setUsername(OTHER_USER);
            other.setEmail("thrd_other@test.com");
            other.setPassword(passwordEncoder.encode("pass123"));
            userRepository.save(other);
        }

        authorToken = jwtService.generateToken(AUTHOR_USER);
        otherToken = jwtService.generateToken(OTHER_USER);
    }

    @Test
    @DisplayName("POST /api/threads/create creates a new thread")
    void testCreateThread() throws Exception {
        String json = "{\"content\":\"Hello from Integration Test!\"}";

        mockMvc.perform(post("/api/threads/create")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("Hello from Integration Test!"))
                .andExpect(jsonPath("$.username").value(AUTHOR_USER));
    }

    @Test
    @DisplayName("GET /api/threads returns paginated list of threads")
    void testGetAllThreads() throws Exception {
        mockMvc.perform(get("/api/threads")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/threads/{id} returns single thread")
    void testGetThreadById() throws Exception {
        Thread thread = new Thread();
        thread.setContent("Thread for get test");
        thread.setUser(author);
        Thread saved = threadRepository.save(thread);

        mockMvc.perform(get("/api/threads/" + saved.getId())
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.content").value("Thread for get test"));
    }

    @Test
    @DisplayName("PUT /api/threads/{id} updates thread when author")
    void testUpdateThread() throws Exception {
        Thread thread = new Thread();
        thread.setContent("Before edit");
        thread.setUser(author);
        Thread saved = threadRepository.save(thread);

        String updateJson = "{\"content\":\"After edit\"}";

        mockMvc.perform(put("/api/threads/" + saved.getId())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("After edit"));
    }

    @Test
    @DisplayName("DELETE /api/threads/{id} deletes thread when author")
    void testDeleteThread() throws Exception {
        Thread thread = new Thread();
        thread.setContent("To be deleted");
        thread.setUser(author);
        Thread saved = threadRepository.save(thread);

        mockMvc.perform(delete("/api/threads/" + saved.getId())
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Thread is successfully deleted"));
    }

    @Test
    @DisplayName("DELETE /api/threads/{id} by non-author returns 400")
    void testDeleteThreadUnauthorized() throws Exception {
        Thread thread = new Thread();
        thread.setContent("Protected thread");
        thread.setUser(author);
        Thread saved = threadRepository.save(thread);

        mockMvc.perform(delete("/api/threads/" + saved.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest());
    }
}
