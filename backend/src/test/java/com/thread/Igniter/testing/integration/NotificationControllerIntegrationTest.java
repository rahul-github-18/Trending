package com.thread.Igniter.testing.integration;

import com.thread.Igniter.notification.entity.Notification;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.repository.NotificationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String NOTIF_RECIPIENT = "notif_rec";
    private static final String NOTIF_SENDER = "notif_snd";
    private String recipientToken;
    private User recipient;
    private User sender;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername(NOTIF_RECIPIENT)) {
            recipient = new User();
            recipient.setUsername(NOTIF_RECIPIENT);
            recipient.setEmail("notif_rec@test.com");
            recipient.setPassword(passwordEncoder.encode("pass123"));
            recipient = userRepository.save(recipient);
        } else {
            recipient = userRepository.findByUsername(NOTIF_RECIPIENT).orElseThrow();
        }

        if (!userRepository.existsByUsername(NOTIF_SENDER)) {
            sender = new User();
            sender.setUsername(NOTIF_SENDER);
            sender.setEmail("notif_snd@test.com");
            sender.setPassword(passwordEncoder.encode("pass123"));
            sender = userRepository.save(sender);
        } else {
            sender = userRepository.findByUsername(NOTIF_SENDER).orElseThrow();
        }

        recipientToken = jwtService.generateToken(NOTIF_RECIPIENT);
    }

    @Test
    @DisplayName("Fetch notifications, get unread count, and mark notification as read")
    void testNotificationWorkflow() throws Exception {
        // Seed an unread notification
        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setRecipient(recipient);
        notification.setType(NotificationType.FOLLOW);
        notification.setMessage("notif_snd started following you");
        notification.setTargetId(recipient.getId());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        // 1. Get notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 2. Get unread count
        mockMvc.perform(get("/api/notifications/unread/count")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk());

        // 3. Mark notification as read
        mockMvc.perform(put("/api/notifications/" + saved.getId() + "/read")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
