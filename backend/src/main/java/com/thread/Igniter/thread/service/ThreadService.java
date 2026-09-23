package com.thread.Igniter.thread.service;

import com.thread.Igniter.common.exception.BadRequestException;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.follow.entity.Follow;
import com.thread.Igniter.follow.repository.FollowRepository;
import com.thread.Igniter.like.repository.LikeRepository;
import com.thread.Igniter.thread.dto.Cursor;
import com.thread.Igniter.thread.dto.CursorPageResponse;
import com.thread.Igniter.thread.dto.ThreadRequestDTO;
import com.thread.Igniter.thread.dto.ThreadResponseDTO;
import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.thread.repository.ThreadRepository;
import com.thread.Igniter.thread.util.CursorUtil;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final StringRedisTemplate redisTemplate;

    private void invalidateFeedCache(String username) {
        Set<String> keys = redisTemplate.keys("feed::" + username + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public ThreadResponseDTO createThread(ThreadRequestDTO request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        Thread thread = new Thread();
        thread.setContent(request.getContent());
        thread.setUser(user);

        Thread savedThread = threadRepository.save(thread);

        invalidateFeedCache(username);

        List<Follow> followers = followRepository.findAllByFollowingId(user.getId());
        for (Follow follow : followers) {
            invalidateFeedCache(follow.getFollower().getUsername());
        }

        ThreadResponseDTO response = new ThreadResponseDTO();
        response.setId(savedThread.getId());
        response.setUsername(savedThread.getUser().getUsername());
        response.setContent(savedThread.getContent());
        response.setCreatedAt(savedThread.getCreatedAt());
        response.setUpdatedAt(savedThread.getUpdatedAt());
        return response;
    }

    private ThreadResponseDTO mapToDTO(Thread thread) {
        ThreadResponseDTO response = new ThreadResponseDTO();
        response.setId(thread.getId());
        response.setUsername(thread.getUser().getUsername());
        response.setContent(thread.getContent());
        response.setCreatedAt(thread.getCreatedAt());
        response.setUpdatedAt(thread.getUpdatedAt());
        response.setLikeCount(thread.getLikeCount());
        response.setCommentCount(thread.getCommentCount());
        return response;
    }

    public Page<ThreadResponseDTO> getAllThreads(String username, Long userId, Pageable pageable) {
        if (userId != null) {
            return threadRepository.findByUserId(userId, pageable).map(this::mapToDTO);
        }
        if (username != null && !username.isBlank()) {
            return threadRepository.findByUserUsername(username, pageable).map(this::mapToDTO);
        }
        return threadRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Page<ThreadResponseDTO> getAllThreads(Pageable pageable) {
        return getAllThreads(null, null, pageable);
    }

    public Page<ThreadResponseDTO> getThreadsByUserId(Long userId, Pageable pageable) {
        return threadRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    public ThreadResponseDTO getThreadById(Long id) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thread Not Found"));

        ThreadResponseDTO response = new ThreadResponseDTO();
        response.setId(thread.getId());
        response.setUsername(thread.getUser().getUsername());
        response.setContent(thread.getContent());
        response.setCreatedAt(thread.getCreatedAt());
        response.setUpdatedAt(thread.getUpdatedAt());
        response.setLikeCount(likeRepository.countByThreadId(thread.getId()));
        return response;
    }

    public ThreadResponseDTO updateThread(Long id, ThreadRequestDTO request, String username) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thread Not Found"));

        boolean isValidUser = username.equals(thread.getUser().getUsername());
        if (!isValidUser) {
            throw new RuntimeException("You are not authorized to update this thread");
        }

        thread.setContent(request.getContent());
        Thread updated = threadRepository.save(thread);

        invalidateFeedCache(username);

        List<Follow> followers = followRepository.findAllByFollowingId(thread.getUser().getId());
        for (Follow follow : followers) {
            invalidateFeedCache(follow.getFollower().getUsername());
        }

        ThreadResponseDTO response = new ThreadResponseDTO();
        response.setId(updated.getId());
        response.setUsername(updated.getUser().getUsername());
        response.setContent(updated.getContent());
        response.setCreatedAt(updated.getCreatedAt());
        response.setUpdatedAt(updated.getUpdatedAt());
        return response;
    }

    @Transactional
    public String deleteThread(Long id, String username) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thread Not Found"));

        boolean isValidUser = username.equals(thread.getUser().getUsername());
        if (!isValidUser) {
            throw new RuntimeException("You are not authorized to delete this thread");
        }

        likeRepository.deleteByThreadId(id);
        threadRepository.delete(thread);

        invalidateFeedCache(username);

        List<Follow> followers = followRepository.findAllByFollowingId(thread.getUser().getId());
        for (Follow follow : followers) {
            invalidateFeedCache(follow.getFollower().getUsername());
        }

        return "Thread is successfully deleted";
    }

    @Cacheable(value = "feed", key = "#username + ':' + #cursor + ':' + #size")
    public CursorPageResponse<ThreadResponseDTO> getFeed(String username, String cursor, int size) {
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Thread> threads;

        if (cursor == null || cursor.isBlank()) {
            threads = threadRepository.findInitialFeedThreads(user.getId(), pageable);
        } else {
            Cursor decodeCursor = CursorUtil.decode(cursor);
            threads = threadRepository.findFeedThreadsAfterCursor(
                    user.getId(),
                    decodeCursor.getCreatedAt(),
                    decodeCursor.getId(),
                    pageable
            );
        }

        boolean hasNext = threads.size() > size;
        if (hasNext) {
            threads = threads.subList(0, size);
        }

        List<ThreadResponseDTO> content = threads.stream().map(thread -> {
            ThreadResponseDTO response = new ThreadResponseDTO();
            response.setId(thread.getId());
            response.setUsername(thread.getUser().getUsername());
            response.setContent(thread.getContent());
            response.setCreatedAt(thread.getCreatedAt());
            response.setUpdatedAt(thread.getUpdatedAt());
            response.setLikeCount(thread.getLikeCount());
            response.setCommentCount(thread.getCommentCount());
            return response;
        }).toList();

        String nextCursor = null;
        if (hasNext && !threads.isEmpty()) {
            Thread lastThread = threads.get(threads.size() - 1);
            Cursor next = new Cursor(lastThread.getId(), lastThread.getCreatedAt());
            nextCursor = CursorUtil.encode(next);
        }

        CursorPageResponse<ThreadResponseDTO> response = new CursorPageResponse<>();
        response.setContent(content);
        response.setNextCursor(nextCursor);
        response.setHasNext(hasNext);
        return response;
    }
}