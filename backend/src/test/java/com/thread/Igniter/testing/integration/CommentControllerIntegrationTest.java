package com.thread.Igniter.testing.integration;

import com.jayway.jsonpath.JsonPath;
import com.thread.Igniter.comment.entity.Comment;
import com.thread.Igniter.comment.repository.CommentRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String COMMENTER_USER = "cmnt_usr";
    private String commenterToken;
    private Thread testThread;
    private User commenter;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername(COMMENTER_USER)) {
            User user = new User();
            user.setUsername(COMMENTER_USER);
            user.setEmail("cmnt_usr@test.com");
            user.setPassword(passwordEncoder.encode("pass123"));
            commenter = userRepository.save(user);
        } else {
            commenter = userRepository.findByUsername(COMMENTER_USER).orElseThrow();
        }

        commenterToken = jwtService.generateToken(COMMENTER_USER);

        Thread thread = new Thread();
        thread.setContent("Thread for comments");
        thread.setUser(commenter);
        testThread = threadRepository.save(thread);
    }

    @Test
    @DisplayName("POST /api/threads/{threadId}/comments creates a comment")
    void testCreateComment() throws Exception {
        String json = "{\"content\":\"Great perspective!\"}";

        mockMvc.perform(post("/api/threads/" + testThread.getId() + "/comments")
                        .header("Authorization", "Bearer " + commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("Great perspective!"))
                .andExpect(jsonPath("$.username").value(COMMENTER_USER));
    }

    @Test
    @DisplayName("GET /api/threads/{threadId}/comments returns comments list")
    void testGetComments() throws Exception {
        Comment comment = new Comment();
        comment.setContent("Existing comment");
        comment.setUser(commenter);
        comment.setThread(testThread);
        commentRepository.save(comment);

        mockMvc.perform(get("/api/threads/" + testThread.getId() + "/comments")
                        .header("Authorization", "Bearer " + commenterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PUT /api/threads/comments/{commentId} updates comment")
    void testUpdateComment() throws Exception {
        Comment comment = new Comment();
        comment.setContent("Before edit comment");
        comment.setUser(commenter);
        comment.setThread(testThread);
        Comment saved = commentRepository.save(comment);

        String json = "{\"content\":\"After edit comment\"}";

        mockMvc.perform(put("/api/threads/comments/" + saved.getId())
                        .header("Authorization", "Bearer " + commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("After edit comment"));
    }

    @Test
    @DisplayName("DELETE /api/threads/comments/{commentId} deletes comment")
    void testDeleteComment() throws Exception {
        Comment comment = new Comment();
        comment.setContent("Delete me");
        comment.setUser(commenter);
        comment.setThread(testThread);
        Comment saved = commentRepository.save(comment);

        mockMvc.perform(delete("/api/threads/comments/" + saved.getId())
                        .header("Authorization", "Bearer " + commenterToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment Deleted"));
    }
}
