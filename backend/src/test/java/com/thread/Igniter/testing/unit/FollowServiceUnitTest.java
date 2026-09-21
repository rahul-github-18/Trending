package com.thread.Igniter.testing.unit;

import com.thread.Igniter.common.exception.BadRequestException;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.follow.entity.Follow;
import com.thread.Igniter.follow.repository.FollowRepository;
import com.thread.Igniter.follow.service.FollowService;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.user.dto.UserResponseDTO;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceUnitTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FollowService followService;

    private User alice;
    private User bob;
    private Follow followRecord;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        followRecord = new Follow();
        followRecord.setId(10L);
        followRecord.setFollower(alice);
        followRecord.setFollowing(bob);
    }

    @Test
    @DisplayName("Should follow user successfully and create notification")
    void testFollowUserSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

        String result = followService.followUser("alice", 2L);

        assertEquals("Followed Successfully", result);
        verify(followRepository).save(any(Follow.class));
        verify(notificationService).createNotification(eq("alice"), eq(2L), eq(NotificationType.FOLLOW), anyString(), eq(2L));
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to follow self")
    void testFollowSelf() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThrows(BadRequestException.class, () -> followService.followUser("alice", 1L));
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when already following")
    void testAlreadyFollowing() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> followService.followUser("alice", 2L));
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should unfollow user successfully and delete notification")
    void testUnfollowUserSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(followRecord));

        String result = followService.unfollowUser("alice", 2L);

        assertEquals("Unfollowed Successfully", result);
        verify(followRepository).delete(followRecord);
        verify(notificationService).deleteFollowNotification(1L, 2L, 2L);
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to unfollow self")
    void testUnfollowSelf() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThrows(BadRequestException.class, () -> followService.unfollowUser("alice", 1L));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trying to unfollow an account not followed")
    void testUnfollowNotFollowed() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> followService.unfollowUser("alice", 2L));
    }

    @Test
    @DisplayName("Should check isFollowing correctly")
    void testIsFollowing() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertTrue(followService.isFollowing("alice", 2L));
    }

    @Test
    @DisplayName("Should return follower and following count")
    void testCounts() {
        when(followRepository.countByFollowingId(2L)).thenReturn(10L);
        when(followRepository.countByFollowerId(2L)).thenReturn(5L);

        assertEquals(10L, followService.getFollowerCount(2L));
        assertEquals(5L, followService.getFollowingCount(2L));
    }

    @Test
    @DisplayName("Should get followers and following lists")
    void testFollowLists() {
        when(followRepository.findAllByFollowingId(2L)).thenReturn(List.of(followRecord));
        when(followRepository.findAllByFollowerId(1L)).thenReturn(List.of(followRecord));

        List<UserResponseDTO> followers = followService.getFollowers(2L);
        List<UserResponseDTO> following = followService.getFollowing(1L);

        assertEquals(1, followers.size());
        assertEquals("alice", followers.get(0).getUsername());

        assertEquals(1, following.size());
        assertEquals("bob", following.get(0).getUsername());
    }
}
