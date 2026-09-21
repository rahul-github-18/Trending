package com.thread.Igniter.testing.unit;

import com.thread.Igniter.comment.dto.CommentRequestDTO;
import com.thread.Igniter.comment.dto.CommentResponseDTO;
import com.thread.Igniter.comment.entity.Comment;
import com.thread.Igniter.comment.repository.CommentRepository;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.common.exception.UnauthorizedException;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import com.thread.Igniter.comment.service.CommentService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceUnitTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User threadAuthor;
    private User commenter;
    private Thread thread;
    private Comment comment;

    @BeforeEach
    void setUp() {
        threadAuthor = new User();
        threadAuthor.setId(1L);
        threadAuthor.setUsername("alice");

        commenter = new User();
        commenter.setId(2L);
        commenter.setUsername("bob");

        thread = new Thread();
        thread.setId(10L);
        thread.setContent("Thread content");
        thread.setUser(threadAuthor);

        comment = new Comment();
        comment.setId(100L);
        comment.setContent("Nice post!");
        comment.setUser(commenter);
        comment.setThread(thread);
        comment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create comment and notify thread author when commenter is different user")
    void testCreateCommentSuccessWithNotification() {
        CommentRequestDTO request = new CommentRequestDTO();
        request.setContent("Nice post!");

        when(threadRepository.findById(10L)).thenReturn(Optional.of(thread));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(commenter));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponseDTO response = commentService.createComment(10L, "bob", request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Nice post!", response.getContent());
        assertEquals("bob", response.getUsername());
        verify(notificationService).createNotification(eq("bob"), eq(1L), eq(NotificationType.COMMENT), anyString(), any());
    }

    @Test
    @DisplayName("Should create comment without notification when commenting on own thread")
    void testCreateCommentOnOwnThread() {
        CommentRequestDTO request = new CommentRequestDTO();
        request.setContent("My own comment");

        Comment ownComment = new Comment();
        ownComment.setId(101L);
        ownComment.setContent("My own comment");
        ownComment.setUser(threadAuthor);
        ownComment.setThread(thread);

        when(threadRepository.findById(10L)).thenReturn(Optional.of(thread));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(threadAuthor));
        when(commentRepository.save(any(Comment.class))).thenReturn(ownComment);

        CommentResponseDTO response = commentService.createComment(10L, "alice", request);

        assertNotNull(response);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when commenting on nonexistent thread")
    void testCreateCommentThreadNotFound() {
        CommentRequestDTO request = new CommentRequestDTO();
        request.setContent("Hello");
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.createComment(99L, "bob", request));
    }

    @Test
    @DisplayName("Should get all comments for a thread")
    void testGetComments() {
        when(commentRepository.findAllByThreadId(10L)).thenReturn(List.of(comment));

        List<CommentResponseDTO> list = commentService.getComments(10L);

        assertEquals(1, list.size());
        assertEquals("Nice post!", list.get(0).getContent());
    }

    @Test
    @DisplayName("Should update comment when authorized")
    void testUpdateCommentSuccess() {
        CommentRequestDTO updateReq = new CommentRequestDTO();
        updateReq.setContent("Updated comment text");

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(commenter));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponseDTO response = commentService.updateComment(100L, "bob", updateReq);

        assertNotNull(response);
        assertEquals("Updated comment text", comment.getContent());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when different user tries to update comment")
    void testUpdateCommentUnauthorized() {
        CommentRequestDTO updateReq = new CommentRequestDTO();
        updateReq.setContent("Hacked text");

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(threadAuthor));

        assertThrows(UnauthorizedException.class, () -> commentService.updateComment(100L, "alice", updateReq));
    }

    @Test
    @DisplayName("Should delete comment and remove its notification when authorized")
    void testDeleteCommentSuccess() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(commenter));

        String result = commentService.deleteComment(100L, "bob");

        assertEquals("Comment Deleted", result);
        verify(commentRepository).deleteById(100L);
        verify(notificationService).deleteCommentNotification(2L, 1L, 100L);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when different user tries to delete comment")
    void testDeleteCommentUnauthorized() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(threadAuthor));

        assertThrows(UnauthorizedException.class, () -> commentService.deleteComment(100L, "alice"));
        verify(commentRepository, never()).deleteById(any());
    }
}
