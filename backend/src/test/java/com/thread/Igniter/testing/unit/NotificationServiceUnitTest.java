package com.thread.Igniter.testing.unit;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.common.exception.UnauthorizedException;
import com.thread.Igniter.notification.dto.NotificationResponseDTO;
import com.thread.Igniter.notification.entity.Notification;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.repository.NotificationRepository;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import com.thread.Igniter.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    private User sender;
    private User recipient;
    private Notification notification;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("alice");

        recipient = new User();
        recipient.setId(2L);
        recipient.setUsername("bob");

        notification = new Notification();
        notification.setId(10L);
        notification.setSender(sender);
        notification.setRecipient(recipient);
        notification.setType(NotificationType.FOLLOW);
        notification.setMessage("alice started following you");
        notification.setTargetId(2L);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create notification successfully")
    void testCreateNotificationSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponseDTO response = notificationService.createNotification(
                "alice", 2L, NotificationType.FOLLOW, "alice started following you", 2L
        );

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("alice", response.getSenderUsername());
        assertEquals(NotificationType.FOLLOW, response.getType());
        assertFalse(response.isRead());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when sender is not found")
    void testCreateNotificationSenderNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.createNotification(
                "unknown", 2L, NotificationType.FOLLOW, "msg", 2L
        ));
    }

    @Test
    @DisplayName("Should get notifications for recipient")
    void testGetNotifications() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(notification));

        List<NotificationResponseDTO> list = notificationService.getNotifications(2L);

        assertEquals(1, list.size());
        assertEquals("alice started following you", list.get(0).getMessage());
    }

    @Test
    @DisplayName("Should get unread notification count")
    void testGetUnreadCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(2L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(2L);

        assertEquals(3L, count);
    }

    @Test
    @DisplayName("Should mark notification as read when authorized recipient")
    void testMarkAsReadSuccess() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        boolean result = notificationService.markAsRead(10L, "bob");

        assertTrue(result);
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when marking someone else's notification as read")
    void testMarkAsReadUnauthorized() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        assertThrows(UnauthorizedException.class, () -> notificationService.markAsRead(10L, "alice"));
    }

    @Test
    @DisplayName("Should delete notifications on unfollow, unlike, and delete comment")
    void testDeleteNotifications() {
        notificationService.deleteFollowNotification(1L, 2L, 2L);
        verify(notificationRepository).deleteBySenderIdAndRecipientIdAndTypeAndTargetId(1L, 2L, NotificationType.FOLLOW, 2L);

        notificationService.deleteLikeNotification(1L, 2L, 100L);
        verify(notificationRepository).deleteBySenderIdAndRecipientIdAndTypeAndTargetId(1L, 2L, NotificationType.LIKE, 100L);

        notificationService.deleteCommentNotification(1L, 2L, 50L);
        verify(notificationRepository).deleteBySenderIdAndRecipientIdAndTypeAndTargetId(1L, 2L, NotificationType.COMMENT, 50L);
    }
}
