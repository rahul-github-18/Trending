package com.thread.Igniter.testing.integration;

import com.jayway.jsonpath.JsonPath;
import com.thread.Igniter.security.service.JwtService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String TEST_USER = "usr_ctrl";
    private String jwtToken;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername(TEST_USER)) {
            User user = new User();
            user.setUsername(TEST_USER);
            user.setEmail("usr_ctrl@test.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setBio("Initial bio");
            userRepository.save(user);
        }
        jwtToken = jwtService.generateToken(TEST_USER);
    }

    @Test
    @DisplayName("POST /api/users should successfully register a new user")
    void testCreateUserSuccess() throws Exception {
        String uniqueName = "u_" + (System.currentTimeMillis() % 100000);
        String json = String.format("{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"pass123\",\"bio\":\"Hey!\"}", uniqueName, uniqueName);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(uniqueName));
    }

    @Test
    @DisplayName("POST /api/users with existing username should return 409 Conflict")
    void testCreateUserDuplicateConflict() throws Exception {
        String json = String.format("{\"username\":\"%s\",\"email\":\"newemail@test.com\",\"password\":\"pass123\"}", TEST_USER);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/users/me with valid JWT returns current user profile")
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USER))
                .andExpect(jsonPath("$.email").value("usr_ctrl@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/me without JWT returns 403/401")
    void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/users/me updates profile info")
    void testUpdateUser() throws Exception {
        String updateJson = String.format("{\"username\":\"%s\",\"email\":\"usr_ctrl@test.com\",\"bio\":\"Updated Bio text\"}", TEST_USER);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated Bio text"));
    }

    @Test
    @DisplayName("POST /api/users/profile-picture uploads image successfully")
    void testUploadProfilePicture() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "simulated-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/users/profile-picture")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/users/search?text= returns matching users")
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("text", "usr_ctrl")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
