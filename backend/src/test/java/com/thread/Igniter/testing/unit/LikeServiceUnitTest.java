package com.thread.Igniter.testing.unit;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.like.dto.LikeResponseDTO;
import com.thread.Igniter.like.entity.Like;
import com.thread.Igniter.like.repository.LikeRepository;
import com.thread.Igniter.like.service.LikeService;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceUnitTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LikeService likeService;

    private User threadAuthor;
    private User liker;
    private Thread thread;
    private Like like;

    @BeforeEach
    void setUp() {
        threadAuthor = new User();
        threadAuthor.setId(1L);
        threadAuthor.setUsername("alice");

        liker = new User();
        liker.setId(2L);
        liker.setUsername("bob");

        thread = new Thread();
        thread.setId(10L);
        thread.setContent("Thread content");
        thread.setUser(threadAuthor);
        thread.setLikeCount(0);

        like = new Like();
        like.setId(100L);
        like.setUser(liker);
        like.setThread(thread);
    }

    @Test
    @DisplayName("Should like thread first time, increment count, and notify author")
    void testLikeThreadFirstTime() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(liker));
        when(threadRepository.findById(10L)).thenReturn(Optional.of(thread));
        when(likeRepository.existsByUserIdAndThreadId(2L, 10L)).thenReturn(false);

        Thread threadAfterIncrement = new Thread();
        threadAfterIncrement.setId(10L);
        threadAfterIncrement.setUser(threadAuthor);
        threadAfterIncrement.setLikeCount(1);
        when(threadRepository.findById(10L)).thenReturn(Optional.of(thread), Optional.of(threadAfterIncrement));

        LikeResponseDTO response = likeService.likeThread(10L, "bob");

        assertTrue(response.isLiked());
        assertEquals(1, response.getLikeCount());
        verify(likeRepository).save(any(Like.class));
        verify(threadRepository).incrementLikeCount(10L);
        verify(notificationService).createNotification(eq("bob"), eq(1L), eq(NotificationType.LIKE), anyString(), eq(10L));
    }

    @Test
    @DisplayName("Should return liked=true without saving again if already liked")
    void testLikeThreadAlreadyLiked() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(liker));
        when(threadRepository.findById(10L)).thenReturn(Optional.of(thread));
        when(likeRepository.existsByUserIdAndThreadId(2L, 10L)).thenReturn(true);

        LikeResponseDTO response = likeService.likeThread(10L, "bob");

        assertTrue(response.isLiked());
        verify(likeRepository, never()).save(any());
        verify(threadRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when liking nonexistent thread")
    void testLikeThreadNotFound() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(liker));
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> likeService.likeThread(99L, "bob"));
    }

    @Test
    @DisplayName("Should delete like, decrement count, and delete notification")
    void testDeleteLikeSuccess() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(liker));
        when(likeRepository.findByUserIdAndThreadId(2L, 10L)).thenReturn(Optional.of(like));

        Thread threadBefore = new Thread();
        threadBefore.setId(10L);
        threadBefore.setUser(threadAuthor);
        threadBefore.setLikeCount(1);

        Thread threadAfter = new Thread();
        threadAfter.setId(10L);
        threadAfter.setUser(threadAuthor);
        threadAfter.setLikeCount(0);

        when(threadRepository.findById(10L)).thenReturn(Optional.of(threadBefore), Optional.of(threadAfter));

        LikeResponseDTO response = likeService.deleteLike(10L, "bob");

        assertFalse(response.isLiked());
        assertEquals(0, response.getLikeCount());
        verify(likeRepository).delete(like);
        verify(threadRepository).decrementLikeCount(10L);
        verify(notificationService).deleteLikeNotification(2L, 1L, 10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trying to delete non-existent like")
    void testDeleteLikeNotFound() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(liker));
        when(likeRepository.findByUserIdAndThreadId(2L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> likeService.deleteLike(10L, "bob"));
    }
}
