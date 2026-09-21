package com.thread.Igniter.like.service;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.like.dto.LikeResponseDTO;
import com.thread.Igniter.like.entity.Like;
import com.thread.Igniter.like.repository.LikeRepository;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final NotificationService notificationService;

    @Transactional
    public LikeResponseDTO likeThread(Long threadId, String username) {

        LikeResponseDTO response = new LikeResponseDTO();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Username Not Found"));

        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Thread Not Found"));

        boolean alreadyLiked =
                likeRepository.existsByUserIdAndThreadId(
                        user.getId(),
                        thread.getId()
                );

        if (alreadyLiked) {
            response.setLiked(true);
            response.setLikeCount(thread.getLikeCount());
            return response;
        }

        Like like = new Like();
        like.setUser(user);
        like.setThread(thread);

        likeRepository.save(like);

        // Atomic increment
        threadRepository.incrementLikeCount(threadId);

        Thread updatedThread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Thread Not Found"));

        response.setLiked(true);
        response.setLikeCount(updatedThread.getLikeCount());

        if (!user.getId().equals(thread.getUser().getId())) {
            notificationService.createNotification(username, thread.getUser().getId(),
                    NotificationType.LIKE, username + " liked your thread", threadId);
        }

        return response;
    }

    @Transactional
    public LikeResponseDTO deleteLike(Long threadId, String username) {

        LikeResponseDTO response = new LikeResponseDTO();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User Not Found"));

        Like like = likeRepository.findByUserIdAndThreadId(
                        user.getId(),
                        threadId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Like Doesn't Exist"));

        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Thread Not Found"));

        likeRepository.delete(like);

        // Atomic decrement
        threadRepository.decrementLikeCount(threadId);

        Thread updatedThread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Thread Not Found"));

        response.setLiked(false);
        response.setLikeCount(updatedThread.getLikeCount());

        notificationService.deleteLikeNotification(user.getId(),thread.getUser().getId(),threadId);

        return response;
    }
}