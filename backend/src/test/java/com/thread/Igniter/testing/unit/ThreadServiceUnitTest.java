package com.thread.Igniter.testing.unit;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.like.repository.LikeRepository;
import com.thread.Igniter.thread.dto.ThreadRequestDTO;
import com.thread.Igniter.thread.dto.ThreadResponseDTO;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.thread.service.ThreadService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadServiceUnitTest {

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private ThreadService threadService;

    private User author;
    private Thread sampleThread;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setUsername("alice");

        sampleThread = new Thread();
        sampleThread.setId(10L);
        sampleThread.setContent("First post on Igniter!");
        sampleThread.setUser(author);
        sampleThread.setLikeCount(0);
        sampleThread.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create thread successfully")
    void testCreateThreadSuccess() {
        ThreadRequestDTO request = new ThreadRequestDTO();
        request.setContent("First post on Igniter!");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(author));
        when(threadRepository.save(any(Thread.class))).thenReturn(sampleThread);

        ThreadResponseDTO response = threadService.createThread(request, "alice");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("alice", response.getUsername());
        assertEquals("First post on Igniter!", response.getContent());
        verify(threadRepository).save(any(Thread.class));
    }

    @Test
    @DisplayName("Should throw exception when creating thread with nonexistent user")
    void testCreateThreadUserNotFound() {
        ThreadRequestDTO request = new ThreadRequestDTO();
        request.setContent("Hello");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> threadService.createThread(request, "ghost"));
    }

    @Test
    @DisplayName("Should get all threads paginated")
    void testGetAllThreads() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Thread> page = new PageImpl<>(List.of(sampleThread));
        when(threadRepository.findAll(pageRequest)).thenReturn(page);

        Page<ThreadResponseDTO> result = threadService.getAllThreads(pageRequest);

        assertEquals(1, result.getTotalElements());
        assertEquals("First post on Igniter!", result.getContent().get(0).getContent());
    }

    @Test
    @DisplayName("Should get thread by ID successfully")
    void testGetThreadByIdSuccess() {
        when(threadRepository.findById(10L)).thenReturn(Optional.of(sampleThread));
        when(likeRepository.countByThreadId(10L)).thenReturn(5L);

        ThreadResponseDTO response = threadService.getThreadById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(5, response.getLikeCount());
    }

    @Test
    @DisplayName("Should throw exception when getting nonexistent thread by ID")
    void testGetThreadByIdNotFound() {
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> threadService.getThreadById(99L));
    }

    @Test
    @DisplayName("Should update thread when authorized author")
    void testUpdateThreadSuccess() {
        ThreadRequestDTO updateReq = new ThreadRequestDTO();
        updateReq.setContent("Updated content");

        when(threadRepository.findById(10L)).thenReturn(Optional.of(sampleThread));
        when(threadRepository.save(any(Thread.class))).thenReturn(sampleThread);

        ThreadResponseDTO response = threadService.updateThread(10L, updateReq, "alice");

        assertNotNull(response);
        assertEquals("Updated content", sampleThread.getContent());
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user tries to update thread")
    void testUpdateThreadUnauthorized() {
        ThreadRequestDTO updateReq = new ThreadRequestDTO();
        updateReq.setContent("Hacked content");

        when(threadRepository.findById(10L)).thenReturn(Optional.of(sampleThread));

        assertThrows(RuntimeException.class, () -> threadService.updateThread(10L, updateReq, "bob"));
    }

    @Test
    @DisplayName("Should delete thread and related likes when authorized")
    void testDeleteThreadSuccess() {
        when(threadRepository.findById(10L)).thenReturn(Optional.of(sampleThread));

        String result = threadService.deleteThread(10L, "alice");

        assertEquals("Thread is successfully deleted", result);
        verify(likeRepository).deleteByThreadId(10L);
        verify(threadRepository).delete(sampleThread);
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user tries to delete thread")
    void testDeleteThreadUnauthorized() {
        when(threadRepository.findById(10L)).thenReturn(Optional.of(sampleThread));

        assertThrows(RuntimeException.class, () -> threadService.deleteThread(10L, "bob"));
        verify(threadRepository, never()).delete(any());
    }
}
