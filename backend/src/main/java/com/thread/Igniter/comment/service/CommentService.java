package com.thread.Igniter.comment.service;

import com.thread.Igniter.comment.dto.CommentRequestDTO;
import com.thread.Igniter.comment.dto.CommentResponseDTO;
import com.thread.Igniter.comment.entity.Comment;
import com.thread.Igniter.comment.repository.CommentRepository;
import com.thread.Igniter.common.exception.BadRequestException;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.common.exception.UnauthorizedException;
import com.thread.Igniter.follow.entity.Follow;
import com.thread.Igniter.follow.repository.FollowRepository;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    private void invalidateFeedCache(String username) {
        Set<String> keys = redisTemplate.keys("feed::" + username + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Transactional
    public CommentResponseDTO createComment(Long threadId, String username, CommentRequestDTO request) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread Not Found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Username does not exist"));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Comment Not Found"));

            if (!parentComment.getThread().getId().equals(threadId)) {
                throw new BadRequestException("Parent comment does not belong to this thread");
            }

            if (parentComment.getParentComment() != null) {
                throw new BadRequestException("Cannot reply to a reply");
            }
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setThread(thread);
        comment.setUser(user);
        comment.setParentComment(parentComment);

        Comment saveComment = commentRepository.save(comment);
        threadRepository.incrementCommentCount(threadId);

        invalidateFeedCache(thread.getUser().getUsername());

        List<Follow> followers = followRepository.findAllByFollowingId(thread.getUser().getId());
        for (Follow follow : followers) {
            invalidateFeedCache(follow.getFollower().getUsername());
        }

        CommentResponseDTO response = new CommentResponseDTO();
        response.setId(saveComment.getId());
        response.setUsername(saveComment.getUser().getUsername());
        response.setContent(saveComment.getContent());
        response.setCreatedAt(saveComment.getCreatedAt());
        response.setUpdatedAt(saveComment.getUpdatedAt());
        response.setParentCommentId(saveComment.getParentComment() != null
                ? saveComment.getParentComment().getId()
                : null);

        if (parentComment == null) {
            if (!user.getId().equals(thread.getUser().getId())) {
                notificationService.createNotification(
                        username,
                        thread.getUser().getId(),
                        NotificationType.COMMENT,
                        username + " commented on your post",
                        comment.getId()
                );
            }
        } else {
            if (!user.getId().equals(parentComment.getUser().getId())) {
                notificationService.createNotification(
                        username,
                        parentComment.getUser().getId(),
                        NotificationType.COMMENT,
                        username + " replied to your comment",
                        comment.getId()
                );
            }
        }

        return response;
    }

    public List<CommentResponseDTO> getComments(Long threadId) {
        return commentRepository.findAllByThreadId(threadId)
                .stream()
                .map(comment -> {
                    CommentResponseDTO response = new CommentResponseDTO();
                    response.setId(comment.getId());
                    response.setUsername(comment.getUser().getUsername());
                    response.setContent(comment.getContent());
                    response.setCreatedAt(comment.getCreatedAt());
                    response.setUpdatedAt(comment.getUpdatedAt());
                    response.setParentCommentId(comment.getParentComment() != null
                            ? comment.getParentComment().getId()
                            : null);
                    return response;
                }).toList();
    }

    public CommentResponseDTO updateComment(Long commentId, String username, CommentRequestDTO request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment does not exist"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Username does not exist"));

        if (!user.getId().equals(comment.getUser().getId())) {
            throw new UnauthorizedException("You are not Authorized to update this comment");
        }

        comment.setContent(request.getContent());
        Comment saveCom = commentRepository.save(comment);

        CommentResponseDTO response = new CommentResponseDTO();
        response.setId(saveCom.getId());
        response.setContent(saveCom.getContent());
        response.setUsername(saveCom.getUser().getUsername());
        response.setCreatedAt(saveCom.getCreatedAt());
        response.setUpdatedAt(saveCom.getUpdatedAt());
        response.setParentCommentId(saveCom.getParentComment() != null
                ? saveCom.getParentComment().getId()
                : null);

        return response;
    }

    @Transactional
    public String deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment does not exist"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Username does not exist"));

        if (!user.getId().equals(comment.getUser().getId())) {
            throw new UnauthorizedException("You are not Authorized to Delete this comment");
        }

        Long threadId = comment.getThread().getId();
        Long recipientId = comment.getParentComment() != null
                ? comment.getParentComment().getUser().getId()
                : comment.getThread().getUser().getId();

        long replyCount = commentRepository.countByParentCommentId(commentId);
        long deleteCount = replyCount + 1;

        commentRepository.deleteAllByParentCommentId(commentId);
        commentRepository.deleteById(commentId);
        threadRepository.decrementCommentCount(threadId, deleteCount);

        invalidateFeedCache(comment.getThread().getUser().getUsername());

        List<Follow> followers = followRepository.findAllByFollowingId(comment.getThread().getUser().getId());
        for (Follow follow : followers) {
            invalidateFeedCache(follow.getFollower().getUsername());
        }

        notificationService.deleteCommentNotification(
                user.getId(),
                recipientId,
                commentId
        );

        return "Comment Deleted";
    }
}